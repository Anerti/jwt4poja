package com.techindna.anerti.validator;

import com.techindna.anerti.dto.LoginInput;
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
}
