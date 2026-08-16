package com.techindna.anerti.repository;

import com.techindna.anerti.repository.model.JExam;
import java.math.BigDecimal;
import java.util.UUID;
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
}
