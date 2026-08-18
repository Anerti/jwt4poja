package com.techindna.anerti.service;

import com.techindna.anerti.dto.StudentGeneralAverage;
import com.techindna.anerti.exception.http.ForbiddenException;
import com.techindna.anerti.exception.http.NotFoundException;
import com.techindna.anerti.exception.http.UnauthorizedException;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.GradeRepository;
import com.techindna.anerti.repository.StudentInheritanceRepository;
import com.techindna.anerti.repository.enums.UserRole;
import com.techindna.anerti.repository.model.JUser;
import com.techindna.anerti.validator.DataValidator;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ReportService {

  private final GradeRepository gradeRepository;
  private final StudentInheritanceRepository studentInheritanceRepository;
  private final AuthRepository authRepository;
  private final DataValidator dataValidator;

  @Transactional(readOnly = true)
  public StudentGeneralAverage getStudentAverage(UUID studentInheritanceId, String academicYear) {
    if (academicYear != null && !academicYear.isBlank()) {
      dataValidator.checkStringLength("academicYear", academicYear, 10);
      dataValidator.validateAcademicYear(academicYear);
    }

    studentInheritanceRepository
        .findById(studentInheritanceId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Student inheritance %s not found".formatted(studentInheritanceId)));

    JUser currentUser = currentUser();
    UUID teacherInheritanceId = null;

    if (currentUser.getRole() == UserRole.STUDENT) {
      if (currentUser.getStudentInheritance() == null
          || !currentUser.getStudentInheritance().getId().equals(studentInheritanceId)) {
        throw new ForbiddenException("Students can only compute their own average.");
      }
    } else if (currentUser.getRole() == UserRole.TEACHER) {
      if (currentUser.getTeacherInheritance() != null) {
        teacherInheritanceId = currentUser.getTeacherInheritance().getId();
      }
    }

    BigDecimal generalAverage =
        gradeRepository.computeGeneralAverage(
            studentInheritanceId,
            academicYear != null && !academicYear.isBlank() ? academicYear : null,
            teacherInheritanceId);

    return new StudentGeneralAverage(studentInheritanceId, academicYear, generalAverage);
  }

  private JUser currentUser() {
    return authRepository
        .findById(
            UUID.fromString(
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal()))
        .orElseThrow(() -> new UnauthorizedException("Authentication required."));
  }
}
