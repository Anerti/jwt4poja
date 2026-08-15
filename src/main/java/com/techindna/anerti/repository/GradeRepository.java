package com.techindna.anerti.repository;

import com.techindna.anerti.repository.model.JGrade;
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
      """
      SELECT g FROM JGrade g, JExam e
      WHERE e.id = g.examId
        AND g.studentInheritance.id = :studentId
        AND (:examId IS NULL OR g.examId = :examId)
        AND (:academicYear IS NULL OR e.academicYear = :academicYear)
        AND (:teacherId IS NULL OR EXISTS (
            SELECT 1 FROM JTeacherCourse tc
            WHERE tc.courseId = e.course.id AND tc.teacherInheritance.id = :teacherId))
      """)
  Page<JGrade> searchByStudent(
      @Param("studentId") UUID studentId,
      @Param("examId") UUID examId,
      @Param("academicYear") Integer academicYear,
      @Param("teacherId") UUID teacherId,
      Pageable pageable);

  boolean existsByStudentInheritanceIdAndExamId(UUID studentInheritanceId, UUID examId);
}
