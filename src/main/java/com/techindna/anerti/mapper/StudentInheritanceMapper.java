package com.techindna.anerti.mapper;

import com.techindna.anerti.dto.CreateStudentInput;
import com.techindna.anerti.dto.StudentInheritance;
import com.techindna.anerti.dto.UserExtendStudent;
import com.techindna.anerti.repository.model.JStudentInheritance;
import com.techindna.anerti.repository.model.JUser;
import org.springframework.stereotype.Component;

@Component
public class StudentInheritanceMapper {

  public JStudentInheritance toRepository(CreateStudentInput request) {
    return JStudentInheritance.builder()
        .ref(request.ref().strip())
        .joinedAt(request.joinedAt())
        .level(request.level())
        .learningPath(request.learningPath())
        .className(request.className())
        .build();
  }

  public UserExtendStudent toDto(JUser jUser) {
    JStudentInheritance inheritance = jUser.getStudentInheritance();
    return new UserExtendStudent(
        jUser.getId(),
        jUser.getUsername(),
        jUser.getFirstName(),
        jUser.getLastName(),
        jUser.getEmail(),
        jUser.getRole(),
        jUser.getCreatedAt(),
        jUser.getUpdatedAt(),
        new StudentInheritance(
            inheritance.getId(),
            inheritance.getRef(),
            inheritance.getJoinedAt(),
            inheritance.getLevel(),
            inheritance.getLearningPath(),
            inheritance.getStudentStatus(),
            inheritance.getGraduationYear(),
            inheritance.getClassName()));
  }
}
