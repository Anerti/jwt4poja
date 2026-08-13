package com.techindna.anerti.service;

import com.techindna.anerti.dto.CreateTeacherInput;
import com.techindna.anerti.dto.UserExtendTeacher;
import com.techindna.anerti.exception.http.ConflictException;
import com.techindna.anerti.mapper.UserMapper;
import com.techindna.anerti.repository.TeacherInheritanceRepository;
import com.techindna.anerti.repository.UserRepository;
import com.techindna.anerti.repository.model.JTeacherInheritance;
import com.techindna.anerti.validator.UserValidator;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class TeacherService {

  private final UserRepository userRepository;
  private final TeacherInheritanceRepository teacherInheritanceRepository;
  private final UserValidator userValidator;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public UserExtendTeacher createTeacher(CreateTeacherInput request) {
    userValidator.validateCreateTeacher(request);

    try {
      JTeacherInheritance inheritance =
          teacherInheritanceRepository.save(userMapper.toJTeacherInheritance(request));

      return userMapper.toUserExtendTeacher(
          userRepository.saveAndFlush(
              userMapper.toJUser(
                  request, passwordEncoder.encode(request.password()), inheritance)));
    } catch (DataIntegrityViolationException e) {
      conflictFrom(e, request);
      throw e;
    }
  }

  private void conflictFrom(DataIntegrityViolationException e, CreateTeacherInput request) {
    String message = e.getMostSpecificCause().getMessage();
    if (message.contains("username")) {
      throw new ConflictException("Cannot use username %s".formatted(request.username().strip()));
    }
    if (message.contains("email")) {
      throw new ConflictException(
          "cannot use email %s".formatted(request.email().strip().toLowerCase()));
    }
    if (message.contains("ref")) {
      throw new ConflictException("Cannot use ref %s".formatted(request.ref().strip()));
    }
  }
}
