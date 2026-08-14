package com.techindna.anerti.mapper;

import com.techindna.anerti.dto.CreateStudentInput;
import com.techindna.anerti.dto.CreateTeacherInput;
import com.techindna.anerti.entity.User;
import com.techindna.anerti.repository.enums.UserRole;
import com.techindna.anerti.repository.model.JStudentInheritance;
import com.techindna.anerti.repository.model.JTeacherInheritance;
import com.techindna.anerti.repository.model.JUser;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

  public JUser toRepository(
      CreateTeacherInput request, String encodedPassword, JTeacherInheritance inheritance) {
    return JUser.builder()
        .username(request.username().strip())
        .password(encodedPassword)
        .firstName(request.firstName())
        .lastName(request.lastName())
        .email(request.email().strip().toLowerCase())
        .role(UserRole.TEACHER)
        .teacherInheritance(inheritance)
        .build();
  }

  public JUser toRepository(
      CreateStudentInput request, String encodedPassword, JStudentInheritance inheritance) {
    return JUser.builder()
        .username(request.username().strip())
        .password(encodedPassword)
        .firstName(request.firstName())
        .lastName(request.lastName())
        .email(request.email().strip().toLowerCase())
        .role(UserRole.STUDENT)
        .studentInheritance(inheritance)
        .build();
  }

  public User toEntity(JUser jUser) {
    return new User(
        jUser.getId(),
        jUser.getUsername(),
        jUser.getFirstName(),
        jUser.getLastName(),
        jUser.getEmail(),
        jUser.getRole(),
        jUser.getCreatedAt(),
        jUser.getUpdatedAt());
  }
}
