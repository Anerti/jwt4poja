package com.techindna.anerti.mapper;

import com.techindna.anerti.dto.CreateTeacherInput;
import com.techindna.anerti.dto.TeacherInheritance;
import com.techindna.anerti.dto.UserExtendTeacher;
import com.techindna.anerti.repository.enums.TeacherStatus;
import com.techindna.anerti.repository.model.JTeacherInheritance;
import com.techindna.anerti.repository.model.JUser;
import org.springframework.stereotype.Component;

@Component
public class TeacherInheritanceMapper {

  public JTeacherInheritance toRepository(CreateTeacherInput request) {
    return JTeacherInheritance.builder()
        .ref(request.ref().strip())
        .joinedAt(request.joinedAt())
        .teacherStatus(
            request.teacherStatus() != null ? request.teacherStatus() : TeacherStatus.ACTIVE)
        .build();
  }

  public UserExtendTeacher toDto(JUser jUser) {
    JTeacherInheritance inheritance = jUser.getTeacherInheritance();
    return new UserExtendTeacher(
        jUser.getId(),
        jUser.getUsername(),
        jUser.getFirstName(),
        jUser.getLastName(),
        jUser.getEmail(),
        jUser.getRole(),
        jUser.getCreatedAt(),
        jUser.getUpdatedAt(),
        new TeacherInheritance(
            inheritance.getId(),
            inheritance.getRef(),
            inheritance.getJoinedAt(),
            inheritance.getTeacherStatus()));
  }
}
