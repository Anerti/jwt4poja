package com.techindna.anerti.mapper;

import com.techindna.anerti.dto.CreateStudentGroupInput;
import com.techindna.anerti.dto.CreateStudentGroupOutput;
import com.techindna.anerti.repository.model.JStudentGroup;
import org.springframework.stereotype.Component;

@Component
public class StudentGroupMapper {

  public JStudentGroup toRepository(CreateStudentGroupInput request) {
    return JStudentGroup.builder()
        .studentInheritanceId(request.studentId())
        .groupId(request.groupId())
        .joinedAt(request.joinedAt())
        .build();
  }

  public CreateStudentGroupOutput toDto(JStudentGroup jStudentGroup) {
    return new CreateStudentGroupOutput(
        jStudentGroup.getStudentInheritanceId(),
        jStudentGroup.getGroupId(),
        jStudentGroup.getJoinedAt(),
        jStudentGroup.getLeftAt());
  }
}
