package com.techindna.anerti.service;

import com.techindna.anerti.dto.CreateGradeInput;
import com.techindna.anerti.dto.GradeListResponse;
import com.techindna.anerti.dto.GradeOutput;
import com.techindna.anerti.dto.Meta;
import com.techindna.anerti.exception.http.ForbiddenException;
import com.techindna.anerti.exception.http.NotFoundException;
import com.techindna.anerti.exception.http.UnauthorizedException;
import com.techindna.anerti.mapper.GradeMapper;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.ExamRepository;
import com.techindna.anerti.repository.GradeRepository;
import com.techindna.anerti.repository.StudentInheritanceRepository;
import com.techindna.anerti.repository.TeacherCourseRepository;
import com.techindna.anerti.repository.enums.UserRole;
import com.techindna.anerti.repository.model.JExam;
import com.techindna.anerti.repository.model.JGrade;
import com.techindna.anerti.repository.model.JStudentInheritance;
import com.techindna.anerti.repository.model.JTeacherInheritance;
import com.techindna.anerti.repository.model.JUser;
import com.techindna.anerti.security.AccessRules;
import com.techindna.anerti.validator.GradeValidator;
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
public class GradeService {

  private final GradeRepository gradeRepository;
  private final StudentInheritanceRepository studentInheritanceRepository;
  private final ExamRepository examRepository;
  private final TeacherCourseRepository teacherCourseRepository;
  private final AuthRepository authRepository;
  private final AccessRules accessRules;
  private final GradeValidator gradeValidator;
  private final GradeMapper gradeMapper;

  @Transactional(readOnly = true)
  public GradeListResponse listGrades(
      UUID studentInheritanceId, String courseRef, String academicYear, int page, int size) {
    gradeValidator.validateListFilters(courseRef, academicYear);

    studentInheritanceRepository
        .findById(studentInheritanceId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Student inheritance %s not found".formatted(studentInheritanceId)));

    JUser currentUser = currentUser();
    PageRequestData p = PageRequestData.of(page, size, Sort.unsorted());

    UUID teacherInheritanceId = null;
    if (currentUser.getRole() == UserRole.STUDENT) {
      JStudentInheritance inheritance = currentUser.getStudentInheritance();
      if (inheritance == null || !inheritance.getId().equals(studentInheritanceId)) {
        throw new ForbiddenException("Students can only view their own grades.");
      }
    } else if (currentUser.getRole() == UserRole.TEACHER) {
      JTeacherInheritance inheritance = currentUser.getTeacherInheritance();
      if (inheritance == null) {
        return new GradeListResponse(List.of(), new Meta(p.page(), p.size(), 0));
      }
      teacherInheritanceId = inheritance.getId();
    }

    Page<JGrade> jGrades =
        gradeRepository.search(
            studentInheritanceId, courseRef, academicYear, teacherInheritanceId, p.pageable());

    List<GradeOutput> grades = jGrades.getContent().stream().map(gradeMapper::toDto).toList();
    return new GradeListResponse(grades, new Meta(p.page(), p.size(), jGrades.getTotalElements()));
  }

  @Transactional
  public GradeOutput createGrade(CreateGradeInput request) {
    gradeValidator.validateCreateGrade(request);

    UUID studentInheritanceId = request.studentInheritanceId();
    UUID examId = request.examId();

    studentInheritanceRepository
        .findById(studentInheritanceId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Student inheritance %s not found".formatted(studentInheritanceId)));

    JExam exam =
        examRepository
            .findById(examId)
            .orElseThrow(() -> new NotFoundException("Exam %s not found".formatted(examId)));

    JUser currentUser = currentUser();
    requireAssignedToCourse(currentUser, exam.getCourseId());

    JGrade saved = gradeRepository.saveAndFlush(gradeMapper.toRepository(request));
    return gradeMapper.toDto(saved);
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
    accessRules.requireAssignedToCourse(
        teacher, courseId, assigned, "Cannot create grade for course %s".formatted(courseId));
  }
}
