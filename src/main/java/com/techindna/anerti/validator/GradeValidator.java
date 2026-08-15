package com.techindna.anerti.validator;

import com.techindna.anerti.dto.CreateGradeInput;
import com.techindna.anerti.dto.UpdateGradeInput;
import com.techindna.anerti.exception.http.UnprocessableContentException;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class GradeValidator {

  private static final BigDecimal MAX_GRADE = BigDecimal.valueOf(20);

  private final DataValidator dataValidator;

  public void validateCreateGrade(CreateGradeInput request) {
    if (request.studentId() == null) {
      throw new UnprocessableContentException("studentId is required and cannot be blank");
    }
    if (request.examId() == null) {
      throw new UnprocessableContentException("examId is required and cannot be blank");
    }
    validateGrade(request.grade());
    if (request.description() != null) {
      dataValidator.checkStringLength("description", request.description(), 500);
    }
  }

  public void validateUpdateGrade(UpdateGradeInput request) {
    validateGrade(request.grade());
    if (request.description() == null || request.description().isBlank()) {
      throw new UnprocessableContentException("description is required and cannot be blank");
    }
    dataValidator.checkStringLength("description", request.description(), 500);
  }

  public void validateListFilters(UUID examId, Integer academicYear) {
    if (academicYear != null && (academicYear < 1900 || academicYear > 2100)) {
      throw new UnprocessableContentException("academicYear must be between 1900 and 2100");
    }
  }

  private void validateGrade(BigDecimal grade) {
    if (grade == null) {
      throw new UnprocessableContentException("grade is required and cannot be blank");
    }
    if (grade.compareTo(BigDecimal.ZERO) < 0 || grade.compareTo(MAX_GRADE) > 0) {
      throw new UnprocessableContentException("grade must be between 0 and 20");
    }
  }
}
