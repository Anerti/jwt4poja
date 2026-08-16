package com.techindna.anerti.repository;

import com.techindna.anerti.repository.model.JExam;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamRepository extends JpaRepository<JExam, UUID> {

  @Query(
      """
      SELECT COALESCE(SUM(e.coefficient), 0) FROM JExam e
      WHERE e.courseId = :courseId AND e.academicYear = :academicYear
      """)
  BigDecimal sumCoefficients(
      @Param("courseId") UUID courseId, @Param("academicYear") String academicYear);

  @Query(
      value =
          """
          SELECT e.id, e.course_id, e.coefficient, e.academic_year, e.date, e.created_at,
                 e.updated_at
          FROM jwt4poja_app.exam e
          JOIN jwt4poja_app.course c ON c.id = e.course_id
          WHERE (CAST(:ref AS text) IS NULL OR :ref = ''
            OR LOWER(c.ref) LIKE LOWER('%' || :ref || '%'))
            AND (CAST(:academicYear AS text) IS NULL OR :academicYear = ''
              OR e.academic_year = :academicYear)
            AND (CAST(:startDate AS timestamptz) IS NULL OR e.date >= :startDate)
            AND (CAST(:endDate AS timestamptz) IS NULL OR e.date <= :endDate)
            AND (CAST(:teacherInheritanceId AS uuid) IS NULL
              OR EXISTS (
                SELECT 1 FROM jwt4poja_app.teacher_course tc
                WHERE tc.teacher_inheritance_id = :teacherInheritanceId
                  AND tc.course_id = e.course_id))
          ORDER BY e.date ASC, e.id ASC
          """,
      countQuery =
          """
          SELECT COUNT(*) FROM jwt4poja_app.exam e
          JOIN jwt4poja_app.course c ON c.id = e.course_id
          WHERE (CAST(:ref AS text) IS NULL OR :ref = ''
            OR LOWER(c.ref) LIKE LOWER('%' || :ref || '%'))
            AND (CAST(:academicYear AS text) IS NULL OR :academicYear = ''
              OR e.academic_year = :academicYear)
            AND (CAST(:startDate AS timestamptz) IS NULL OR e.date >= :startDate)
            AND (CAST(:endDate AS timestamptz) IS NULL OR e.date <= :endDate)
            AND (CAST(:teacherInheritanceId AS uuid) IS NULL
              OR EXISTS (
                SELECT 1 FROM jwt4poja_app.teacher_course tc
                WHERE tc.teacher_inheritance_id = :teacherInheritanceId
                  AND tc.course_id = e.course_id))
          """,
      nativeQuery = true)
  Page<JExam> search(
      @Param("ref") String ref,
      @Param("academicYear") String academicYear,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate,
      @Param("teacherInheritanceId") UUID teacherInheritanceId,
      Pageable pageable);
}
