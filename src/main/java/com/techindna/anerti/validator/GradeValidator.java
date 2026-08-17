package com.techindna.anerti.validator;

import com.techindna.anerti.dto.CreateGradeInput;
import com.techindna.anerti.exception.http.BadRequestException;
import com.techindna.anerti.exception.http.UnprocessableContentException;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class GradeValidator {

  private final DataValidator dataValidator;

  private static final BigDecimal MIN_VALUE = BigDecimal.ZERO;
  private static final BigDecimal MAX_VALUE = new BigDecimal("20");

  public void validateCreateGrade(CreateGradeInput request) {
    if (request.studentInheritanceId() == null) {
      throw new UnprocessableContentException(
          "studentInheritanceId is required and cannot be blank");
    }
    if (request.examId() == null) {
      throw new UnprocessableContentException("examId is required and cannot be blank");
    }
    validateValue(request.value());
    if (request.description() == null || request.description().isBlank()) {
      throw new UnprocessableContentException("description is required and cannot be blank");
    }
  }

  public void validateListFilters(String courseRef, String academicYear) {
    if (courseRef != null && !courseRef.isBlank()) {
      dataValidator.checkStringLength("courseRef", courseRef, 10);
      dataValidator.validateRef(courseRef);
    }
    if (academicYear != null && !academicYear.isBlank()) {
      dataValidator.checkStringLength("academicYear", academicYear, 10);
      dataValidator.validateAcademicYear(academicYear);
    }
  }

  private void validateValue(BigDecimal value) {
    if (value == null) {
      throw new BadRequestException("value is required and cannot be blank");
    }
    if (value.compareTo(MIN_VALUE) < 0) {
      throw new BadRequestException("value must be greater than or equal to 0");
    }
    if (value.compareTo(MAX_VALUE) > 0) {
      throw new UnprocessableContentException("value must not exceed 20");
    }
    if (value.stripTrailingZeros().scale() > 2) {
      throw new UnprocessableContentException("value must have at most 2 decimal places");
    }
  }
}
