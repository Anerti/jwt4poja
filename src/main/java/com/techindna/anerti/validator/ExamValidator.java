package com.techindna.anerti.validator;

import com.techindna.anerti.dto.CreateExamInput;
import com.techindna.anerti.exception.http.UnprocessableContentException;
import java.math.BigDecimal;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ExamValidator {

  private static final Pattern ACADEMIC_YEAR_FORMAT = Pattern.compile("^\\d{4}-\\d{4}$");

  private final DataValidator dataValidator;

  public void validateCreateExam(CreateExamInput request) {
    if (request.courseId() == null) {
      throw new UnprocessableContentException("courseId is required and cannot be blank");
    }
    validateCoefficient(request.coefficient());
    validateAcademicYear(request.academicYear());
    if (request.date() == null) {
      throw new UnprocessableContentException("date is required and cannot be blank");
    }
  }

  private void validateCoefficient(BigDecimal coefficient) {
    if (coefficient == null) {
      throw new UnprocessableContentException("coefficient is required and cannot be blank");
    }
    if (coefficient.compareTo(BigDecimal.ZERO) <= 0) {
      throw new UnprocessableContentException("coefficient must be greater than 0");
    }
    if (coefficient.compareTo(BigDecimal.ONE) > 0) {
      throw new UnprocessableContentException("coefficient must not exceed 1");
    }
    if (coefficient.stripTrailingZeros().scale() > 2) {
      throw new UnprocessableContentException("coefficient must have at most 2 decimal places");
    }
  }

  private void validateAcademicYear(String academicYear) {
    dataValidator.checkNullData("academicYear", academicYear);
    dataValidator.checkStringLength("academicYear", academicYear, 10);

    if (!ACADEMIC_YEAR_FORMAT.matcher(academicYear.strip()).matches()) {
      throw new UnprocessableContentException(
          "academicYear is invalid, it must follow the format YYYY-YYYY (e.g. 2024-2025)");
    }
  }
}
