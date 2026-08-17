package com.techindna.anerti.validator;

import com.techindna.anerti.dto.CreateClassInput;
import com.techindna.anerti.exception.http.BadRequestException;
import com.techindna.anerti.exception.http.UnprocessableContentException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ClassValidator {

  private final DataValidator dataValidator;

  public void validateCreateClass(CreateClassInput request) {
    if (request.name() == null || request.name().isBlank()) {
      throw new UnprocessableContentException("name is required and cannot be blank");
    }
    dataValidator.checkStringLength("name", request.name(), 30);

    if (request.yearOf() == null) {
      throw new UnprocessableContentException("yearOf is required and cannot be blank");
    }
    if (request.yearOf() < 2000 || request.yearOf() > 2100) {
      throw new BadRequestException("yearOf must be between 2000 and 2100");
    }
  }

  public void validateListFilters(String search) {
    if (search != null && !search.isBlank()) {
      dataValidator.checkStringLength("search", search, 30);
    }
  }
}
