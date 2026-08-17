package com.techindna.anerti.mapper;

import com.techindna.anerti.dto.CreateGradeInput;
import com.techindna.anerti.dto.GradeOutput;
import com.techindna.anerti.repository.model.JGrade;
import org.springframework.stereotype.Component;

@Component
public class GradeMapper {

  public JGrade toRepository(CreateGradeInput request) {
    return JGrade.builder()
        .studentInheritanceId(request.studentInheritanceId())
        .examId(request.examId())
        .value(request.value())
        .description(request.description().strip())
        .build();
  }

  public GradeOutput toDto(JGrade jGrade) {
    return new GradeOutput(
        jGrade.getId(),
        jGrade.getStudentInheritanceId(),
        jGrade.getExamId(),
        jGrade.getValue(),
        jGrade.getDescription(),
        jGrade.getCreatedAt());
  }
}
