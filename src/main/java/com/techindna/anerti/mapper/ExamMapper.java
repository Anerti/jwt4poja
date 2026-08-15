package com.techindna.anerti.mapper;

import com.techindna.anerti.dto.CreateExamInput;
import com.techindna.anerti.dto.ExamOutput;
import com.techindna.anerti.entity.Exam;
import com.techindna.anerti.repository.model.JCourse;
import com.techindna.anerti.repository.model.JExam;
import org.springframework.stereotype.Component;

@Component
public class ExamMapper {

  public JExam toRepository(CreateExamInput request, JCourse course) {
    return JExam.builder()
        .course(course)
        .coefficient(request.coefficient())
        .academicYear(request.academicYear())
        .date(request.date())
        .build();
  }

  public Exam toEntity(JExam jExam) {
    return new Exam(
        jExam.getId(),
        jExam.getCourse().getId(),
        jExam.getCoefficient(),
        jExam.getAcademicYear(),
        jExam.getDate(),
        jExam.getCreatedAt(),
        jExam.getUpdatedAt());
  }

  public ExamOutput toDto(Exam exam) {
    return new ExamOutput(
        exam.id(),
        exam.courseId(),
        exam.coefficient(),
        exam.academicYear(),
        exam.date(),
        exam.createdAt(),
        exam.updatedAt());
  }
}
