package com.techindna.anerti.validator;

import com.techindna.anerti.dto.CreateTeacherCourseInput;
import com.techindna.anerti.exception.http.UnprocessableContentException;
import org.springframework.stereotype.Component;

@Component
public class TeacherCourseValidator {

  public void validateCreateTeacherCourse(CreateTeacherCourseInput request) {
    if (request.teacherId() == null) {
      throw new UnprocessableContentException("teacherId is required and cannot be blank");
    }
    if (request.courseId() == null) {
      throw new UnprocessableContentException("courseId is required and cannot be blank");
    }
    if (request.assignedAt() == null) {
      throw new UnprocessableContentException("assignedAt is required and cannot be blank");
    }
  }
}
