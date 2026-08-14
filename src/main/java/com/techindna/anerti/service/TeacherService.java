package com.techindna.anerti.service;

import com.techindna.anerti.dto.CreateTeacherInput;
import com.techindna.anerti.dto.Meta;
import com.techindna.anerti.dto.TeacherListResponse;
import com.techindna.anerti.dto.UserExtendTeacher;
import com.techindna.anerti.mapper.TeacherInheritanceMapper;
import com.techindna.anerti.mapper.UserMapper;
import com.techindna.anerti.repository.TeacherInheritanceRepository;
import com.techindna.anerti.repository.UserRepository;
import com.techindna.anerti.repository.enums.TeacherStatus;
import com.techindna.anerti.repository.enums.UserRole;
import com.techindna.anerti.repository.model.JTeacherInheritance;
import com.techindna.anerti.repository.model.JUser;
import com.techindna.anerti.validator.DataValidator;
import com.techindna.anerti.validator.UserValidator;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class TeacherService {

  private final UserRepository userRepository;
  private final TeacherInheritanceRepository teacherInheritanceRepository;
  private final UserValidator userValidator;
  private final DataValidator dataValidator;
  private final UserMapper userMapper;
  private final TeacherInheritanceMapper teacherInheritanceMapper;
  private final UserConflictHandler userConflictHandler;
  private final PasswordEncoder passwordEncoder;

  @Transactional(readOnly = true)
  public TeacherListResponse listTeachers(
      String search, TeacherStatus teacherStatus, int page, int size) {
    if (search != null && !search.isBlank()) {
      dataValidator.validateSearchString(search);
    }

    int validPage = defaultIfInvalid(page, 1, 100, 1);
    int validSize = defaultIfInvalid(size, 1, 100, 10);
    Pageable pageable = PageRequest.of(validPage - 1, validSize, Sort.by("createdAt", "username"));

    Page<JUser> jUsers =
        userRepository.searchTeachers(
            search,
            teacherStatus == null ? null : teacherStatus.name(),
            UserRole.TEACHER,
            pageable);

    List<UserExtendTeacher> teachers =
        jUsers.getContent().stream().map(teacherInheritanceMapper::toDto).toList();
    return new TeacherListResponse(
        teachers, new Meta(validPage, validSize, jUsers.getTotalElements()));
  }

  private int defaultIfInvalid(int value, int min, int max, int defaultValue) {
    return (value < min || value > max) ? defaultValue : value;
  }

  @Transactional
  public UserExtendTeacher createTeacher(CreateTeacherInput request) {
    userValidator.validateCreateTeacher(request);

    try {
      JTeacherInheritance inheritance =
          teacherInheritanceRepository.save(teacherInheritanceMapper.toRepository(request));

      return teacherInheritanceMapper.toDto(
          userRepository.saveAndFlush(
              userMapper.toRepository(
                  request, passwordEncoder.encode(request.password()), inheritance)));
    } catch (DataIntegrityViolationException e) {
      userConflictHandler.conflictFrom(e, request.username(), request.email(), request.ref());
      throw e;
    }
  }
}
