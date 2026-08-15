package com.techindna.anerti.repository;

import com.techindna.anerti.repository.model.JExam;
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
      SELECT e FROM JExam e
      WHERE (:teacherId IS NULL OR EXISTS (
          SELECT 1 FROM JTeacherCourse tc
          WHERE tc.courseId = e.course.id AND tc.teacherInheritance.id = :teacherId))
        AND (:ref IS NULL OR :ref = ''
          OR LOWER(e.course.ref) LIKE LOWER(CONCAT('%', CAST(:ref AS string), '%')))
        AND (:academicYear IS NULL OR e.academicYear = :academicYear)
        AND (:startDate IS NULL OR e.date >= :startDate)
        AND (:endDate IS NULL OR e.date <= :endDate)
      """)
  Page<JExam> search(
      @Param("teacherId") UUID teacherId,
      @Param("ref") String ref,
      @Param("academicYear") Integer academicYear,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate,
      Pageable pageable);
}
