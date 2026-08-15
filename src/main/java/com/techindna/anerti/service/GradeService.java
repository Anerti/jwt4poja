package com.techindna.anerti.service;

import com.techindna.anerti.dto.CreateGradeInput;
import com.techindna.anerti.dto.GradeListResponse;
import com.techindna.anerti.dto.GradeOutput;
import com.techindna.anerti.dto.HistoryListResponse;
import com.techindna.anerti.dto.Meta;
import com.techindna.anerti.dto.UpdateGradeInput;
import com.techindna.anerti.entity.HistoryEntry;
import com.techindna.anerti.exception.http.ConflictException;
import com.techindna.anerti.exception.http.ForbiddenException;
import com.techindna.anerti.exception.http.NotFoundException;
import com.techindna.anerti.mapper.GradeMapper;
import com.techindna.anerti.repository.ExamRepository;
import com.techindna.anerti.repository.GradeHistoryRepository;
import com.techindna.anerti.repository.GradeRepository;
import com.techindna.anerti.repository.StudentInheritanceRepository;
import com.techindna.anerti.repository.TeacherCourseRepository;
import com.techindna.anerti.repository.enums.UserRole;
import com.techindna.anerti.repository.model.JExam;
import com.techindna.anerti.repository.model.JGrade;
import com.techindna.anerti.repository.model.JGradeHistory;
import com.techindna.anerti.repository.model.JUser;
import com.techindna.anerti.validator.GradeValidator;
import java.math.BigDecimal;
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
public class GradeService {

  private final GradeRepository gradeRepository;
  private final GradeHistoryRepository gradeHistoryRepository;
  private final ExamRepository examRepository;
  private final StudentInheritanceRepository studentInheritanceRepository;
  private final TeacherCourseRepository teacherCourseRepository;
  private final UserContext userContext;
  private final GradeValidator gradeValidator;
  private final GradeMapper gradeMapper;

  @Transactional
  public GradeOutput createGrade(CreateGradeInput request) {
    gradeValidator.validateCreateGrade(request);
    JUser currentUser = userContext.currentUser();
    JExam exam = findExam(request.examId());
    if (currentUser.getRole() == UserRole.TEACHER) {
      ensureTeacherAssignedToCourse(currentUser, exam.getCourse().getId());
    }
    if (gradeRepository.existsByStudentInheritanceIdAndExamId(
        request.studentId(), request.examId())) {
      throw new ConflictException(
          "Student %s already has a grade for exam %s"
              .formatted(request.studentId(), request.examId()));
    }

    try {
      JGrade saved =
          gradeRepository.saveAndFlush(
              gradeMapper.toRepository(
                  request, studentInheritanceRepository.getReferenceById(request.studentId())));
      gradeHistoryRepository.save(
          JGradeHistory.builder()
              .gradeId(saved.getId())
              .grade(request.grade())
              .description(request.description() == null ? "" : request.description().strip())
              .build());
      return new GradeOutput(
          saved.getId(),
          request.studentId(),
          request.examId(),
          request.grade(),
          saved.getCreatedAt());
    } catch (DataIntegrityViolationException e) {
      String message = e.getMostSpecificCause().getMessage();
      if (message.contains("grade_student_inheritance_id_fkey")) {
        throw new NotFoundException("Student %s not found".formatted(request.studentId()));
      }
      if (message.contains("grade_student_inheritance_id_exam_id_key")) {
        throw new ConflictException(
            "Student %s already has a grade for exam %s"
                .formatted(request.studentId(), request.examId()));
      }
      throw e;
    }
  }

  @Transactional(readOnly = true)
  public GradeListResponse listGrades(
      UUID studentId, UUID examId, Integer academicYear, int page, int size) {
    gradeValidator.validateListFilters(examId, academicYear);

    JUser currentUser = userContext.currentUser();
    UUID forcedStudentId =
        currentUser.getRole() == UserRole.STUDENT
            ? currentUser.getStudentInheritance().getId()
            : studentId;
    UUID teacherId =
        currentUser.getRole() == UserRole.TEACHER
            ? currentUser.getTeacherInheritance().getId()
            : null;

    PageRequestData p = PageRequestData.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    Page<JGrade> jGrades =
        gradeRepository.searchByStudent(
            forcedStudentId, examId, academicYear, teacherId, p.pageable());

    List<GradeOutput> grades = jGrades.getContent().stream().map(this::toOutput).toList();
    return new GradeListResponse(grades, new Meta(p.page(), p.size(), jGrades.getTotalElements()));
  }

  @Transactional
  public GradeOutput updateGrade(UUID gradeId, UpdateGradeInput request) {
    gradeValidator.validateUpdateGrade(request);
    JUser currentUser = userContext.currentUser();
    JGrade grade = findGrade(gradeId);
    if (currentUser.getRole() == UserRole.TEACHER) {
      ensureTeacherAssignedToCourse(currentUser, findExam(grade.getExamId()).getCourse().getId());
    }

    gradeHistoryRepository.save(
        JGradeHistory.builder()
            .gradeId(gradeId)
            .grade(request.grade())
            .description(request.description().strip())
            .build());
    return new GradeOutput(
        gradeId,
        grade.getStudentInheritance().getId(),
        grade.getExamId(),
        request.grade(),
        grade.getCreatedAt());
  }

  @Transactional(readOnly = true)
  public HistoryListResponse getGradeHistory(UUID gradeId) {
    JUser currentUser = userContext.currentUser();
    JGrade grade = findGrade(gradeId);
    if (currentUser.getRole() == UserRole.STUDENT) {
      UUID studentId = currentUser.getStudentInheritance().getId();
      if (!studentId.equals(grade.getStudentInheritance().getId())) {
        throw new ForbiddenException(
            "Student %s cannot access grade %s".formatted(studentId, gradeId));
      }
    } else if (currentUser.getRole() == UserRole.TEACHER) {
      ensureTeacherAssignedToCourse(currentUser, findExam(grade.getExamId()).getCourse().getId());
    }

    List<HistoryEntry> history =
        gradeHistoryRepository.findByGradeIdOrderByCreatedAtAsc(gradeId).stream()
            .map(gradeMapper::toHistoryEntity)
            .toList();
    return new HistoryListResponse(history);
  }

  private GradeOutput toOutput(JGrade jGrade) {
    BigDecimal currentGrade =
        gradeHistoryRepository
            .findTopByGradeIdOrderByCreatedAtDesc(jGrade.getId())
            .map(JGradeHistory::getGrade)
            .orElse(BigDecimal.ZERO);
    return new GradeOutput(
        jGrade.getId(),
        jGrade.getStudentInheritance().getId(),
        jGrade.getExamId(),
        currentGrade,
        jGrade.getCreatedAt());
  }

  private JExam findExam(UUID examId) {
    return examRepository
        .findById(examId)
        .orElseThrow(() -> new NotFoundException("Exam %s not found".formatted(examId)));
  }

  private JGrade findGrade(UUID gradeId) {
    return gradeRepository
        .findById(gradeId)
        .orElseThrow(() -> new NotFoundException("Grade %s not found".formatted(gradeId)));
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
