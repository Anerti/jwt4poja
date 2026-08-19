package com.techindna.anerti.repository;

import com.techindna.anerti.repository.model.JClass;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<JClass, UUID> {

  @Query(
      value =
          """
WITH latest_grades AS (
    SELECT g.student_inheritance_id, g.value, e.coefficient, e.course_id, e.id AS exam_id,
           ROW_NUMBER() OVER (PARTITION BY g.exam_id ORDER BY g.created_at DESC) rn
    FROM jwt4poja_app.grade g
    JOIN jwt4poja_app.exam e ON e.id = g.exam_id
),
valid_grades AS (
    SELECT student_inheritance_id, value, coefficient, course_id
    FROM latest_grades WHERE rn = 1
),
course_averages AS (
    SELECT student_inheritance_id, course_id,
           SUM(value * coefficient) AS course_weighted_average
    FROM valid_grades
    GROUP BY student_inheritance_id, course_id
),
student_general AS (
    SELECT ca.student_inheritance_id,
           CASE WHEN SUM(c.credits) > 0
                THEN SUM(ca.course_weighted_average * c.credits) / SUM(c.credits)
                ELSE 0 END AS general_average
    FROM course_averages ca
    JOIN jwt4poja_app.course c ON c.id = ca.course_id
    GROUP BY ca.student_inheritance_id
)
SELECT ROW_NUMBER() OVER (ORDER BY COALESCE(sg.general_average, 0) DESC) AS "rank",
       si.id AS student_id, u.first_name, u.last_name, si.ref,
       COALESCE(sg.general_average, 0) AS general_average
FROM jwt4poja_app.student_inheritance si
JOIN jwt4poja_app."user" u ON u.student_inheritance_id = si.id
LEFT JOIN student_general sg ON sg.student_inheritance_id = si.id
WHERE si.class_id = ?1
ORDER BY COALESCE(sg.general_average, 0) DESC
""",
      nativeQuery = true)
  List<Object[]> findClassRanking(@Param("classId") UUID classId);
}
