package com.techindna.anerti.service;

import com.techindna.anerti.dto.CreateStudentInput;
import com.techindna.anerti.dto.Meta;
import com.techindna.anerti.dto.StudentListResponse;
import com.techindna.anerti.dto.UserExtendStudent;
import com.techindna.anerti.mapper.StudentInheritanceMapper;
import com.techindna.anerti.mapper.UserMapper;
import com.techindna.anerti.repository.StudentInheritanceRepository;
import com.techindna.anerti.repository.UserRepository;
import com.techindna.anerti.repository.enums.LearningPath;
import com.techindna.anerti.repository.enums.StudentStatus;
import com.techindna.anerti.repository.enums.UserRole;
import com.techindna.anerti.repository.model.JStudentInheritance;
import com.techindna.anerti.repository.model.JUser;
import com.techindna.anerti.validator.DataValidator;
import com.techindna.anerti.validator.UserValidator;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class StudentService {

  private final UserRepository userRepository;
  private final StudentInheritanceRepository studentInheritanceRepository;
  private final UserValidator userValidator;
  private final DataValidator dataValidator;
  private final UserMapper userMapper;
  private final StudentInheritanceMapper studentInheritanceMapper;
  private final UserConflictHandler userConflictHandler;
  private final PasswordEncoder passwordEncoder;

  @Transactional(readOnly = true)
  public StudentListResponse listStudents(
      String search,
      LearningPath learningPath,
      StudentStatus studentStatus,
      String className,
      int page,
      int size) {
    if (search != null && !search.isBlank()) {
      dataValidator.validateSearchString(search);
    }

    if (className != null && !className.isBlank()) {
      dataValidator.validateSearchString(className);
    }

    PageRequestData p = PageRequestData.of(page, size, Sort.by("createdAt", "username"));

    Page<JUser> jUsers =
        userRepository.searchStudents(
            search,
            learningPath == null ? null : learningPath.name(),
            studentStatus == null ? null : studentStatus.name(),
            className,
            UserRole.STUDENT,
            p.pageable());

    List<UserExtendStudent> students =
        jUsers.getContent().stream().map(studentInheritanceMapper::toDto).toList();
    return new StudentListResponse(
        students, new Meta(p.page(), p.size(), jUsers.getTotalElements()));
  }

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
