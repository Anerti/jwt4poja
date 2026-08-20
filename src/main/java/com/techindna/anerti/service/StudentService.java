package com.techindna.anerti.service;

import com.techindna.anerti.dto.CreateStudentInput;
import com.techindna.anerti.dto.EnrollClassInput;
import com.techindna.anerti.dto.EnrollmentRejected;
import com.techindna.anerti.dto.FailingCourse;
import com.techindna.anerti.dto.Meta;
import com.techindna.anerti.dto.StudentListResponse;
import com.techindna.anerti.dto.UserExtendStudent;
import com.techindna.anerti.exception.http.EnrollmentRejectedException;
import com.techindna.anerti.exception.http.NotFoundException;
import com.techindna.anerti.mapper.StudentInheritanceMapper;
import com.techindna.anerti.mapper.UserMapper;
import com.techindna.anerti.repository.ClassRepository;
import com.techindna.anerti.repository.GradeRepository;
import com.techindna.anerti.repository.StudentInheritanceRepository;
import com.techindna.anerti.repository.UserRepository;
import com.techindna.anerti.repository.enums.LearningPath;
import com.techindna.anerti.repository.enums.Level;
import com.techindna.anerti.repository.enums.StudentStatus;
import com.techindna.anerti.repository.enums.UserRole;
import com.techindna.anerti.repository.model.JStudentInheritance;
import com.techindna.anerti.repository.model.JUser;
import com.techindna.anerti.validator.StudentValidator;
import java.math.BigDecimal;
import java.util.ArrayList;
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
  private final ClassRepository classRepository;
  private final GradeRepository gradeRepository;
  private final StudentValidator studentValidator;
  private final UserMapper userMapper;
  private final StudentInheritanceMapper studentInheritanceMapper;
  private final UserConflictHandler userConflictHandler;
  private final PasswordEncoder passwordEncoder;

  @Transactional(readOnly = true)
  public StudentListResponse listStudents(
      String search,
      Level level,
      LearningPath learningPath,
      StudentStatus studentStatus,
      String groupRef,
      String className,
      int page,
      int size) {
    studentValidator.validateListFilters(search, groupRef, className);

    PageRequestData p = PageRequestData.of(page, size, Sort.by("createdAt", "username"));

    Page<JUser> jUsers =
        userRepository.searchStudents(
            search,
            level == null ? null : level.name(),
            learningPath == null ? null : learningPath.name(),
            studentStatus == null ? null : studentStatus.name(),
            groupRef,
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
    studentValidator.validateCreateStudent(request);

    try {
      JStudentInheritance inheritance =
          studentInheritanceRepository.save(studentInheritanceMapper.toRepository(request));

      return studentInheritanceMapper.toDto(
          userRepository.saveAndFlush(
              userMapper.toRepository(
                  request, passwordEncoder.encode(request.password()), inheritance)));
    } catch (DataIntegrityViolationException e) {
      String message = e.getMostSpecificCause().getMessage();
      if (message.contains("group_id")) {
        throw new NotFoundException("Group %s not found".formatted(request.groupId()));
      }
      userConflictHandler.conflictFrom(e, request.username(), request.email(), request.ref());
      throw e;
    }
  }

  @Transactional
  public UserExtendStudent enrollStudentInClass(
      java.util.UUID studentId, EnrollClassInput request) {
    studentValidator.validateEnrollStudent(request);

    JUser jUser =
        userRepository
            .findById(studentId)
            .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));

    if (jUser.getStudentInheritance() == null) {
      throw new NotFoundException("Student inheritance not found for: " + studentId);
    }

    classRepository
        .findById(request.classId())
        .orElseThrow(() -> new NotFoundException("Class not found: " + request.classId()));

    List<Object[]> failingRows =
        gradeRepository.findFailingCourses(jUser.getStudentInheritance().getId());

    if (!failingRows.isEmpty()) {
      List<FailingCourse> failingCourses = new ArrayList<>();
      for (Object[] row : failingRows) {
        failingCourses.add(
            new FailingCourse((String) row[0], (String) row[1], (BigDecimal) row[2]));
      }
      throw new EnrollmentRejectedException(
          new EnrollmentRejected(
              studentId,
              "Enrollment rejected: %d course(s) with average below 10"
                  .formatted(failingCourses.size()),
              failingCourses));
    }

    jUser.getStudentInheritance().setClassId(request.classId());
    userRepository.saveAndFlush(jUser);

    return studentInheritanceMapper.toDto(jUser);
  }
}
