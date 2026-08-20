package com.techindna.anerti.service;

import com.techindna.anerti.dto.GradeReportInput;
import com.techindna.anerti.dto.GradeReportResponse;
import com.techindna.anerti.endpoint.event.EventProducer;
import com.techindna.anerti.endpoint.event.model.GradeReportRequested;
import com.techindna.anerti.exception.http.ConflictException;
import com.techindna.anerti.exception.http.ForbiddenException;
import com.techindna.anerti.exception.http.NotFoundException;
import com.techindna.anerti.exception.http.UnauthorizedException;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.GradeRepository;
import com.techindna.anerti.repository.StudentInheritanceRepository;
import com.techindna.anerti.repository.enums.UserRole;
import com.techindna.anerti.repository.model.JStudentInheritance;
import com.techindna.anerti.repository.model.JUser;
import com.techindna.anerti.validator.GradeReportValidator;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class GradeReportService {

  private final AuthRepository authRepository;
  private final StudentInheritanceRepository studentInheritanceRepository;
  private final GradeRepository gradeRepository;
  private final GradeReportValidator gradeReportValidator;
  private final EventProducer<GradeReportRequested> eventProducer;

  @Transactional
  public GradeReportResponse generateGradeReport(GradeReportInput request) {
    gradeReportValidator.validate(request);

    UUID studentInheritanceId = request.studentInheritanceId();
    String academicYear = request.academicYear().strip();

    JStudentInheritance studentInheritance =
        studentInheritanceRepository
            .findById(studentInheritanceId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Student inheritance %s not found".formatted(studentInheritanceId)));

    JUser currentUser = currentUser();

    if (currentUser.getRole() == UserRole.STUDENT) {
      JStudentInheritance studentOwn =
          currentUser.getStudentInheritance();
      if (studentOwn == null || !studentOwn.getId().equals(studentInheritanceId)) {
        throw new ForbiddenException("Students can only generate their own reports.");
      }
    }

    boolean hasGrades =
        gradeRepository.existsByStudentInheritanceIdAndAcademicYear(
            studentInheritanceId, academicYear);
    if (!hasGrades) {
      throw new ConflictException(
          "Student has no grades for academic year %s".formatted(academicYear));
    }

    if (studentInheritance.getClassId() == null) {
      throw new ConflictException("Student is not enrolled in any class.");
    }

    GradeReportRequested event =
        GradeReportRequested.builder()
            .studentInheritanceId(studentInheritanceId)
            .academicYear(academicYear)
            .recipientEmail(currentUser.getEmail())
            .recipientFirstName(currentUser.getFirstName())
            .recipientLastName(currentUser.getLastName())
            .build();
    eventProducer.accept(List.of(event));

    return new GradeReportResponse(
        "Grade report for academic year %s will be sent to your email shortly"
            .formatted(academicYear),
        studentInheritanceId,
        academicYear);
  }

  private JUser currentUser() {
    return authRepository
        .findById(
            UUID.fromString(
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal()))
        .orElseThrow(() -> new UnauthorizedException("Authentication required."));
  }
}
