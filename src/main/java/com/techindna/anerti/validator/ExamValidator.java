package com.techindna.anerti.validator;

import com.techindna.anerti.dto.CreateExamInput;
import com.techindna.anerti.exception.http.UnprocessableContentException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ExamValidator {

  private final DataValidator dataValidator;

  public void validateCreateExam(CreateExamInput request) {
    if (request.courseId() == null) {
      throw new UnprocessableContentException("courseId is required and cannot be blank");
    }
    dataValidator.validateCoefficient(request.coefficient());
    dataValidator.validateAcademicYear(request.academicYear());
    if (request.date() == null) {
      throw new UnprocessableContentException("date is required and cannot be blank");
    }
  }
}
