package com.techindna.anerti.repository;

import com.techindna.anerti.repository.model.JClass;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GraduationRepository extends JpaRepository<JClass, UUID> {

  @Query(
      value =
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
          WHERE si.class_id = ?1 AND si.student_status = 'GRADUATED'
          ORDER BY COALESCE(sa.overall_average, 0) DESC
          """,
      nativeQuery = true)
  List<Object[]> findGraduates(@Param("classId") UUID classId);
}
