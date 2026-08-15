package com.techindna.anerti.service;

import com.techindna.anerti.dto.*;
import com.techindna.anerti.exception.http.ConflictException;
import com.techindna.anerti.exception.http.NotFoundException;
import com.techindna.anerti.mapper.StudentGroupMapper;
import com.techindna.anerti.repository.StudentGroupRepository;
import com.techindna.anerti.repository.model.JStudentGroup;
import com.techindna.anerti.validator.StudentGroupValidator;
import java.sql.SQLException;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class StudentGroupService {

  private static final String UNIQUE_CONSTRAINT_VIOLATION_CODE = "23505";
  private static final String FOREIGN_KEY_VIOLATION_CODE = "23503";

  private final StudentGroupRepository studentGroupRepository;
  private final StudentGroupValidator studentGroupValidator;
  private final StudentGroupMapper studentGroupMapper;

  @Transactional
  public CreateStudentGroupListResponse enrollStudents(CreateStudentGroupRequest request) {
    studentGroupValidator.validateCreate(request);

    List<CreateStudentGroupOutput> outputs =
        request.data().stream().map(this::createEnrollment).toList();
    return new CreateStudentGroupListResponse(outputs, new Meta(1, outputs.size(), outputs.size()));
  }

  private CreateStudentGroupOutput createEnrollment(CreateStudentGroupInput item) {
    try {
      JStudentGroup saved =
          studentGroupRepository.saveAndFlush(studentGroupMapper.toRepository(item));
      
      return studentGroupMapper.toDto(saved);
    } catch (DataIntegrityViolationException e) {
      if (sqlState(e, UNIQUE_CONSTRAINT_VIOLATION_CODE)) {
        throw new ConflictException(
            "Student %s is already enrolled in group %s"
                .formatted(item.studentId(), item.groupId()));
      }
      if (sqlState(e, FOREIGN_KEY_VIOLATION_CODE)) {
        throw new NotFoundException(
            "Student %s or group %s not found".formatted(item.studentId(), item.groupId()));
      }
      throw e;
    }
  }

  private static boolean sqlState(DataIntegrityViolationException e, String state) {
    return e.getRootCause() instanceof SQLException sqlEx && state.equals(sqlEx.getSQLState());
  }
}
