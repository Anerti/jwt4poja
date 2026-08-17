package com.techindna.anerti.repository;

import com.techindna.anerti.repository.model.JGrade;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GradeRepository extends JpaRepository<JGrade, UUID> {

  @Query(
      value =
          """
          SELECT g.id, g.student_inheritance_id, g.exam_id, g.value, g.description, g.created_at
          FROM jwt4poja_app.grade g
          JOIN jwt4poja_app.exam e ON e.id = g.exam_id
          JOIN jwt4poja_app.course c ON c.id = e.course_id
          WHERE g.student_inheritance_id = :studentInheritanceId
            AND (CAST(:courseRef AS text) IS NULL OR :courseRef = ''
              OR LOWER(c.ref) LIKE LOWER('%' || :courseRef || '%'))
            AND (CAST(:academicYear AS text) IS NULL OR :academicYear = ''
              OR e.academic_year = :academicYear)
            AND (CAST(:teacherInheritanceId AS uuid) IS NULL
              OR EXISTS (
                SELECT 1 FROM jwt4poja_app.teacher_course tc
                WHERE tc.teacher_inheritance_id = :teacherInheritanceId
                  AND tc.course_id = e.course_id))
          ORDER BY g.created_at DESC, g.id ASC
          """,
      countQuery =
          """
          SELECT COUNT(*)
          FROM jwt4poja_app.grade g
          JOIN jwt4poja_app.exam e ON e.id = g.exam_id
          JOIN jwt4poja_app.course c ON c.id = e.course_id
          WHERE g.student_inheritance_id = :studentInheritanceId
            AND (CAST(:courseRef AS text) IS NULL OR :courseRef = ''
              OR LOWER(c.ref) LIKE LOWER('%' || :courseRef || '%'))
            AND (CAST(:academicYear AS text) IS NULL OR :academicYear = ''
              OR e.academic_year = :academicYear)
            AND (CAST(:teacherInheritanceId AS uuid) IS NULL
              OR EXISTS (
                SELECT 1 FROM jwt4poja_app.teacher_course tc
                WHERE tc.teacher_inheritance_id = :teacherInheritanceId
                  AND tc.course_id = e.course_id))
          """,
      nativeQuery = true)
  Page<JGrade> search(
      @Param("studentInheritanceId") UUID studentInheritanceId,
      @Param("courseRef") String courseRef,
      @Param("academicYear") String academicYear,
      @Param("teacherInheritanceId") UUID teacherInheritanceId,
      Pageable pageable);

  @Query(
      value =
          """
          WITH latest_grades AS (
            SELECT g.value, e.coefficient,
                   ROW_NUMBER() OVER (PARTITION BY g.exam_id ORDER BY g.created_at DESC) rn
            FROM jwt4poja_app.grade g
            JOIN jwt4poja_app.exam e ON e.id = g.exam_id
            JOIN jwt4poja_app.course c ON c.id = e.course_id
            WHERE g.student_inheritance_id = :studentInheritanceId
              AND LOWER(c.ref) = LOWER(:courseRef)
          )
          SELECT COALESCE(SUM(value * coefficient), 0) AS weighted_average
          FROM latest_grades
          WHERE rn = 1
          """,
      nativeQuery = true)
  BigDecimal computeCourseGrade(
      @Param("studentInheritanceId") UUID studentInheritanceId,
      @Param("courseRef") String courseRef);
}
