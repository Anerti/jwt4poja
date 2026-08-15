package com.techindna.anerti.validator;

import com.techindna.anerti.dto.CreateStudentInput;
import com.techindna.anerti.dto.CreateStudentRequest;
import com.techindna.anerti.dto.CreateTeacherInput;
import com.techindna.anerti.dto.LoginInput;
import com.techindna.anerti.exception.http.BadRequestException;
import com.techindna.anerti.exception.http.UnprocessableContentException;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UserValidator {

  private final DataValidator dataValidator;

  public void validateCreateTeacher(CreateTeacherInput request) {
    validateUserAccount(
        request.username(),
        request.password(),
        request.firstName(),
        request.lastName(),
        request.email(),
        request.ref());
  }

  public void validateCreateStudents(CreateStudentRequest request) {
    List<CreateStudentInput> data = request.data();
    if (data == null || data.isEmpty()) {
      throw new BadRequestException("data must contain at least one student");
    }
    if (data.size() > 10) {
      throw new UnprocessableContentException("data must not contain more than 10 students");
    }
    data.forEach(this::validateCreateStudent);
  }

  public void validateCreateStudent(CreateStudentInput request) {
    validateUserAccount(
        request.username(),
        request.password(),
        request.firstName(),
        request.lastName(),
        request.email(),
        request.ref());

    if (request.level() == null) {
      throw new UnprocessableContentException("level is required and cannot be blank");
    }

    if (request.learningPath() == null) {
      throw new UnprocessableContentException("learningPath is required and cannot be blank");
    }
  }

  private void validateUserAccount(
      String username,
      String password,
      String firstName,
      String lastName,
      String email,
      String ref) {
    dataValidator.validateUsername(username);
    dataValidator.checkPasswordSecurityLevel(password);
    dataValidator.validateName("firstName", firstName);
    dataValidator.validateName("lastName", lastName);
    dataValidator.validateEmail("email", email);
    dataValidator.validateRef(ref);
  }

  public void validateLogin(LoginInput request) {
    if (request.email() != null && !request.email().isBlank()) {
      dataValidator.validateEmail("email", request.email());
    }
    if (request.username() != null && !request.username().isBlank()) {
      dataValidator.validateUsername(request.username());
    }
    if ((request.username() == null || request.username().isBlank())
        && (request.email() == null || request.email().isBlank())) {
      throw new UnprocessableContentException("Username or email is required and cannot be blank");
    }
    dataValidator.checkNullData("password", request.password());
  }
}
