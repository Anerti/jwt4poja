package com.techindna.anerti.validator;

import com.techindna.anerti.dto.CreateExamInput;
import com.techindna.anerti.exception.http.UnprocessableContentException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ExamValidator {

  private static final Pattern REF_FORMAT = Pattern.compile("^[A-Za-z0-9-]+$");

  private final DataValidator dataValidator;

  public void validateCreateExam(CreateExamInput request) {
    if (request.courseId() == null) {
      throw new UnprocessableContentException("courseId is required and cannot be blank");
    }
    validateCoefficient(request.coefficient());
    validateAcademicYear(request.academicYear());
    if (request.academicYear() == null) {
      throw new UnprocessableContentException("academicYear is required and cannot be blank");
    }
    if (request.date() == null) {
      throw new UnprocessableContentException("date is required and cannot be blank");
    }
  }

  public void validateListFilters(
      String ref, Integer academicYear, Instant startDate, Instant endDate) {
    if (ref != null && !ref.isBlank()) {
      dataValidator.checkStringLength("ref", ref, 10);
      if (!REF_FORMAT.matcher(ref).matches()) {
        throw new UnprocessableContentException(
            "Ref %s is invalid, it may only contain letters, numbers, and the symbol -"
                .formatted(ref));
      }
    }
    validateAcademicYear(academicYear);
    if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
      throw new UnprocessableContentException("startDate must be before or equal to endDate");
    }
  }

  private void validateCoefficient(BigDecimal coefficient) {
    if (coefficient == null) {
      throw new UnprocessableContentException("coefficient is required and cannot be blank");
    }
    if (coefficient.compareTo(BigDecimal.ZERO) <= 0) {
      throw new UnprocessableContentException("coefficient must be greater than 0");
    }
  }

  private void validateAcademicYear(Integer academicYear) {
    if (academicYear != null && (academicYear < 1900 || academicYear > 2100)) {
      throw new UnprocessableContentException("academicYear must be between 1900 and 2100");
    }
  }
}
