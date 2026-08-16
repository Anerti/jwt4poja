package com.techindna.anerti.service;

import com.techindna.anerti.dto.CreateExamInput;
import com.techindna.anerti.dto.ExamListResponse;
import com.techindna.anerti.dto.ExamOutput;
import com.techindna.anerti.dto.Meta;
import com.techindna.anerti.exception.http.ConflictException;
import com.techindna.anerti.exception.http.NotFoundException;
import com.techindna.anerti.exception.http.UnauthorizedException;
import com.techindna.anerti.mapper.ExamMapper;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.CourseRepository;
import com.techindna.anerti.repository.ExamRepository;
import com.techindna.anerti.repository.TeacherCourseRepository;
import com.techindna.anerti.repository.enums.UserRole;
import com.techindna.anerti.repository.model.JExam;
import com.techindna.anerti.repository.model.JTeacherInheritance;
import com.techindna.anerti.repository.model.JUser;
import com.techindna.anerti.security.AccessRules;
import com.techindna.anerti.validator.ExamValidator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
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

  @Transactional(readOnly = true)
  public ExamListResponse listExams(
      String ref, String academicYear, Instant startDate, Instant endDate, int page, int size) {
    examValidator.validateListFilters(ref, academicYear);
    JUser currentUser = currentUser();

    PageRequestData p = PageRequestData.of(page, size, Sort.unsorted());

    UUID teacherInheritanceId = null;
    if (currentUser.getRole() == UserRole.TEACHER) {
      JTeacherInheritance inheritance = currentUser.getTeacherInheritance();
      if (inheritance == null) {
        return new ExamListResponse(List.of(), new Meta(p.page(), p.size(), 0));
      }
      teacherInheritanceId = inheritance.getId();
    }

    Page<JExam> jExams =
        examRepository.search(
            ref, academicYear, startDate, endDate, teacherInheritanceId, p.pageable());

    List<ExamOutput> exams = jExams.getContent().stream().map(examMapper::toDto).toList();
    return new ExamListResponse(exams, new Meta(p.page(), p.size(), jExams.getTotalElements()));
  }

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
          "Coefficients of the exam of course %s for academic year %s already sum to %s"
              .formatted(courseId, academicYear, sum));
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
