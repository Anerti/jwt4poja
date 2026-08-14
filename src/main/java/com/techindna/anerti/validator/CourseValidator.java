package com.techindna.anerti.validator;

import com.techindna.anerti.dto.CreateCourseInput;
import com.techindna.anerti.exception.http.UnprocessableContentException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CourseValidator {

  private final DataValidator dataValidator;

  public void validateCreateCourse(CreateCourseInput request) {
    dataValidator.validateRef(request.ref());
    dataValidator.validateTitle(request.title());
    validateCredits(request.credits());
  }

  public void validateListFilters(String search) {
    if (search != null && !search.isBlank()) {
      dataValidator.validateSearchString(search);
    }
  }

  private void validateCredits(Integer credits) {
    if (credits == null) {
      throw new UnprocessableContentException("credits is required and cannot be blank");
    }
    if (credits < 1 || credits > 30) {
      throw new UnprocessableContentException("credits must be between 1 and 30");
    }
  }
}
