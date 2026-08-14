package com.techindna.anerti.validator;

import com.techindna.anerti.dto.CreateStudentGroupInput;
import com.techindna.anerti.dto.CreateStudentGroupRequest;
import com.techindna.anerti.exception.http.BadRequestException;
import org.springframework.stereotype.Component;

@Component
public class StudentGroupValidator {

  public void validateCreate(CreateStudentGroupRequest request) {
    if (request.data() == null) {
      throw new BadRequestException("data is required and cannot be blank");
    }
    for (CreateStudentGroupInput item : request.data()) {
      if (item.studentId() == null) {
        throw new BadRequestException("studentId is required and cannot be blank");
      }
      if (item.groupId() == null) {
        throw new BadRequestException("groupId is required and cannot be blank");
      }
      if (item.joinedAt() == null) {
        throw new BadRequestException("joinedAt is required and cannot be blank");
      }
    }
  }
}
