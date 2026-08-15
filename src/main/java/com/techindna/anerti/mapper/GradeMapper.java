package com.techindna.anerti.mapper;

import com.techindna.anerti.dto.CreateGradeInput;
import com.techindna.anerti.entity.HistoryEntry;
import com.techindna.anerti.repository.model.JGrade;
import com.techindna.anerti.repository.model.JGradeHistory;
import com.techindna.anerti.repository.model.JStudentInheritance;
import org.springframework.stereotype.Component;

@Component
public class GradeMapper {

  public JGrade toRepository(CreateGradeInput request, JStudentInheritance student) {
    return JGrade.builder().studentInheritance(student).examId(request.examId()).build();
  }

  public HistoryEntry toHistoryEntity(JGradeHistory jGradeHistory) {
    return new HistoryEntry(
        jGradeHistory.getId(),
        jGradeHistory.getGrade(),
        jGradeHistory.getDescription(),
        jGradeHistory.getCreatedAt());
  }
}
