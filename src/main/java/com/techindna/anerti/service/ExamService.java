package com.techindna.anerti.service;

import com.techindna.anerti.dto.CreateExamInput;
import com.techindna.anerti.dto.ExamOutput;
import com.techindna.anerti.exception.http.ConflictException;
import com.techindna.anerti.exception.http.NotFoundException;
import com.techindna.anerti.exception.http.UnauthorizedException;
import com.techindna.anerti.mapper.ExamMapper;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.CourseRepository;
import com.techindna.anerti.repository.ExamRepository;
import com.techindna.anerti.repository.TeacherCourseRepository;
import com.techindna.anerti.repository.model.JExam;
import com.techindna.anerti.repository.model.JTeacherInheritance;
import com.techindna.anerti.repository.model.JUser;
import com.techindna.anerti.security.AccessRules;
import com.techindna.anerti.validator.ExamValidator;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ExamService {

  private static final BigDecimal ONE = BigDecimal.ONE;

  private final ExamRepository examRepository;
  private final CourseRepository courseRepository;
  private final AuthRepository authRepository;
  private final TeacherCourseRepository teacherCourseRepository;
  private final AccessRules accessRules;
  private final ExamValidator examValidator;
  private final ExamMapper examMapper;

  @Transactional
  public ExamOutput createExam(CreateExamInput request) {
    examValidator.validateCreateExam(request);

    UUID courseId = request.courseId();

    courseRepository
        .findById(courseId)
        .orElseThrow(() -> new NotFoundException("Course %s not found".formatted(courseId)));

    JUser currentUser = currentUser();
    requireAssignedToCourse(currentUser, courseId);

    String academicYear = request.academicYear().strip();
    BigDecimal sum = examRepository.sumCoefficients(courseId, academicYear);
    sum = sum == null ? BigDecimal.ZERO : sum;

    if (sum.add(request.coefficient()).compareTo(ONE) > 0) {
      throw new ConflictException(
          "Coefficients of the exams of course %s for academic year %s already sum to %s, adding %s"
              .formatted(courseId, academicYear, sum, request.coefficient()));
    }

    JExam saved = examRepository.saveAndFlush(examMapper.toRepository(request));
    return examMapper.toDto(saved);
  }

  private JUser currentUser() {
    return authRepository
        .findById(
            UUID.fromString(
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal()))
        .orElseThrow(() -> new UnauthorizedException("Authentication required."));
  }

  private void requireAssignedToCourse(JUser teacher, UUID courseId) {
    JTeacherInheritance inheritance = teacher.getTeacherInheritance();
    boolean assigned =
        inheritance != null
            && teacherCourseRepository.existsByTeacherInheritanceIdAndCourseId(
                inheritance.getId(), courseId);
    accessRules.requireAssignedToCourse(teacher, courseId, assigned);
  }
}
