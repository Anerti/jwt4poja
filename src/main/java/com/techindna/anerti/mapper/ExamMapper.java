package com.techindna.anerti.mapper;

import com.techindna.anerti.dto.CreateExamInput;
import com.techindna.anerti.dto.ExamOutput;
import com.techindna.anerti.repository.model.JExam;
import org.springframework.stereotype.Component;

@Component
public class ExamMapper {

  public JExam toRepository(CreateExamInput request) {
    return JExam.builder()
        .courseId(request.courseId())
        .coefficient(request.coefficient())
        .academicYear(request.academicYear().strip())
        .date(request.date())
        .build();
  }

  public ExamOutput toDto(JExam jExam) {
    return new ExamOutput(
        jExam.getId(),
        jExam.getCourseId(),
        jExam.getCoefficient(),
        jExam.getAcademicYear(),
        jExam.getDate(),
        jExam.getCreatedAt(),
        jExam.getUpdatedAt());
  }
}
