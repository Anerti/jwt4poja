package com.techindna.anerti.validator;

import com.techindna.anerti.dto.GradeReportInput;
import com.techindna.anerti.exception.http.UnprocessableContentException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class GradeReportValidator {

  private final DataValidator dataValidator;

  public void validate(GradeReportInput request) {
    if (request.studentInheritanceId() == null) {
      throw new UnprocessableContentException(
          "studentInheritanceId is required and cannot be blank");
    }
    dataValidator.validateAcademicYear(request.academicYear());
  }
}
