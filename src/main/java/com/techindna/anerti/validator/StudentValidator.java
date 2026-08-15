package com.techindna.anerti.validator;

import com.techindna.anerti.dto.CreateStudentInput;
import com.techindna.anerti.exception.http.UnprocessableContentException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class StudentValidator {

  private final DataValidator dataValidator;

  public void validateCreateStudent(CreateStudentInput request) {
    dataValidator.validateUserAccount(
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

  public void validateListFilters(String search, String groupRef, String className) {
    if (search != null && !search.isBlank()) {
      dataValidator.validateSearchString(search);
    }

    if (groupRef != null && !groupRef.isBlank()) {
      dataValidator.validateRef(groupRef);
    }

    if (className != null && !className.isBlank()) {
      dataValidator.checkStringLength("className", className, 30);
      dataValidator.validateSearchString(className);
    }
  }
}
