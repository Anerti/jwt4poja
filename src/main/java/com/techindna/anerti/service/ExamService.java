package com.techindna.anerti.service;

import com.techindna.anerti.dto.CreateExamInput;
import com.techindna.anerti.dto.ExamListResponse;
import com.techindna.anerti.dto.ExamOutput;
import com.techindna.anerti.dto.Meta;
import com.techindna.anerti.exception.http.ForbiddenException;
import com.techindna.anerti.exception.http.NotFoundException;
import com.techindna.anerti.mapper.ExamMapper;
import com.techindna.anerti.repository.CourseRepository;
import com.techindna.anerti.repository.ExamRepository;
import com.techindna.anerti.repository.TeacherCourseRepository;
import com.techindna.anerti.repository.enums.UserRole;
import com.techindna.anerti.repository.model.JExam;
import com.techindna.anerti.repository.model.JUser;
import com.techindna.anerti.validator.ExamValidator;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ExamService {

  private final ExamRepository examRepository;
  private final CourseRepository courseRepository;
  private final TeacherCourseRepository teacherCourseRepository;
  private final UserContext userContext;
  private final ExamValidator examValidator;
  private final ExamMapper examMapper;

  @Transactional
  public ExamOutput createExam(CreateExamInput request) {
    examValidator.validateCreateExam(request);
    JUser currentUser = userContext.currentUser();
    if (currentUser.getRole() == UserRole.TEACHER) {
      ensureTeacherAssignedToCourse(currentUser, request.courseId());
    }

    try {
      JExam saved =
          examRepository.saveAndFlush(
              examMapper.toRepository(
                  request, courseRepository.getReferenceById(request.courseId())));
      return examMapper.toDto(examMapper.toEntity(saved));
    } catch (DataIntegrityViolationException e) {
      if (e.getMostSpecificCause().getMessage().contains("course_id")) {
        throw new NotFoundException("Course %s not found".formatted(request.courseId()));
      }
      throw e;
    }
  }

  @Transactional(readOnly = true)
  public ExamListResponse listExams(
      String ref, Integer academicYear, Instant startDate, Instant endDate, int page, int size) {
    examValidator.validateListFilters(ref, academicYear, startDate, endDate);

    JUser currentUser = userContext.currentUser();
    UUID teacherId =
        currentUser.getRole() == UserRole.TEACHER
            ? currentUser.getTeacherInheritance().getId()
            : null;

    PageRequestData p = PageRequestData.of(page, size, Sort.by(Sort.Direction.DESC, "date"));
    Page<JExam> jExams =
        examRepository.search(teacherId, ref, academicYear, startDate, endDate, p.pageable());

    List<ExamOutput> exams =
        jExams.getContent().stream().map(examMapper::toEntity).map(examMapper::toDto).toList();
    return new ExamListResponse(exams, new Meta(p.page(), p.size(), jExams.getTotalElements()));
  }

  private void ensureTeacherAssignedToCourse(JUser teacher, UUID courseId) {
    UUID teacherInheritanceId = teacher.getTeacherInheritance().getId();
    boolean assigned =
        teacherCourseRepository.existsByTeacherInheritanceIdAndCourseId(
            teacherInheritanceId, courseId);
    if (!assigned) {
      throw new ForbiddenException(
          "Teacher %s is not assigned to course %s".formatted(teacherInheritanceId, courseId));
    }
  }
}
