package com.techindna.anerti.validator;

import com.techindna.anerti.dto.LoginInput;
import com.techindna.anerti.dto.RegisterInput;
import com.techindna.anerti.exception.http.UnprocessableContentException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UserValidator {

  private final DataValidator dataValidator;

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

  public void validateRegistration(RegisterInput request) {
    dataValidator.validateEmail("email", request.email());

    dataValidator.checkPasswordSecurityLevel(request.password());

    dataValidator.checkNullData("confirmPassword", request.confirmPassword());
    if (!request.password().equals(request.confirmPassword())) {
      throw new UnprocessableContentException("Passwords do not match");
    }

    dataValidator.validateName("firstName", request.firstName());
    dataValidator.validateName("lastName", request.lastName());

    dataValidator.validateUsername(request.username());
  }
}
