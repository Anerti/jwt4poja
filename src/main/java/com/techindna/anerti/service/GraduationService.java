package com.techindna.anerti.service;

import com.techindna.anerti.dto.GraduateOutput;
import com.techindna.anerti.dto.GraduationListResponse;
import com.techindna.anerti.dto.Meta;
import com.techindna.anerti.exception.http.NotFoundException;
import com.techindna.anerti.repository.ClassRepository;
import com.techindna.anerti.repository.model.JClass;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class GraduationService {

  private final JdbcTemplate jdbcTemplate;
  private final ClassRepository classRepository;

  @Transactional(readOnly = true)
  public GraduationListResponse listGraduates(UUID classId) {
    JClass clazz =
        classRepository
            .findById(classId)
            .orElseThrow(() -> new NotFoundException("Class %s not found".formatted(classId)));

    List<GraduateOutput> graduates =
        jdbcTemplate.query(
            """
            WITH latest_grades AS (
                SELECT g.student_inheritance_id, g.value, e.coefficient, e.id AS exam_id,
                       ROW_NUMBER() OVER (PARTITION BY g.exam_id ORDER BY g.created_at DESC) rn
                FROM jwt4poja_app.grade g
                JOIN jwt4poja_app.exam e ON e.id = g.exam_id
            ),
            valid_grades AS (
                SELECT student_inheritance_id, value, coefficient
                FROM latest_grades WHERE rn = 1
            ),
            student_averages AS (
                SELECT student_inheritance_id,
                       CASE WHEN SUM(coefficient) > 0
                            THEN SUM(value * coefficient) / SUM(coefficient)
                            ELSE 0 END AS overall_average
                FROM valid_grades
                GROUP BY student_inheritance_id
            )
            SELECT ROW_NUMBER() OVER (ORDER BY COALESCE(sa.overall_average, 0) DESC) AS "rank",
                   si.id AS student_id, u.first_name, u.last_name,
                   COALESCE(sa.overall_average, 0) AS average
            FROM jwt4poja_app.student_inheritance si
            JOIN jwt4poja_app."user" u ON u.student_inheritance_id = si.id
            LEFT JOIN student_averages sa ON sa.student_inheritance_id = si.id
            WHERE si.class_id = ? AND si.student_status = 'GRADUATED'
            ORDER BY COALESCE(sa.overall_average, 0) DESC
            """,
            (rs, rowNum) ->
                new GraduateOutput(
                    rs.getInt("rank"),
                    rs.getObject("student_id", UUID.class),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    new BigDecimal(rs.getString("average")).setScale(2)),
            classId);

    return new GraduationListResponse(
        clazz.getName(),
        clazz.getYearOf(),
        graduates,
        new Meta(1, graduates.size(), graduates.size()));
  }
}
