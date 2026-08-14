package com.techindna.anerti.service;

import com.techindna.anerti.dto.CreateStudentInput;
import com.techindna.anerti.dto.UserExtendStudent;
import com.techindna.anerti.mapper.StudentInheritanceMapper;
import com.techindna.anerti.mapper.UserMapper;
import com.techindna.anerti.repository.StudentInheritanceRepository;
import com.techindna.anerti.repository.UserRepository;
import com.techindna.anerti.repository.model.JStudentInheritance;
import com.techindna.anerti.validator.UserValidator;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class StudentService {

  private final UserRepository userRepository;
  private final StudentInheritanceRepository studentInheritanceRepository;
  private final UserValidator userValidator;
  private final UserMapper userMapper;
  private final StudentInheritanceMapper studentInheritanceMapper;
  private final UserConflictHandler userConflictHandler;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public UserExtendStudent createStudent(CreateStudentInput request) {
    userValidator.validateCreateStudent(request);

    try {
      JStudentInheritance inheritance =
          studentInheritanceRepository.save(studentInheritanceMapper.toRepository(request));

      return studentInheritanceMapper.toDto(
          userRepository.saveAndFlush(
              userMapper.toRepository(
                  request, passwordEncoder.encode(request.password()), inheritance)));
    } catch (DataIntegrityViolationException e) {
      userConflictHandler.conflictFrom(e, request.username(), request.email(), request.ref());
      throw e;
    }
  }
}
