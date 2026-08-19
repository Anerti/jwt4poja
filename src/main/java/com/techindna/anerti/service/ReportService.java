package com.techindna.anerti.service;

import com.techindna.anerti.dto.GradeReportInput;
import com.techindna.anerti.dto.GradeReportResponse;
import com.techindna.anerti.dto.ClassRankingEntry;
import com.techindna.anerti.dto.ClassRankingResponse;
import com.techindna.anerti.dto.StudentGeneralAverage;
import com.techindna.anerti.endpoint.event.EventProducer;
import com.techindna.anerti.endpoint.event.model.GradeReportRequested;
import com.techindna.anerti.exception.http.ConflictException;
import com.techindna.anerti.exception.http.ForbiddenException;
import com.techindna.anerti.exception.http.NotFoundException;
import com.techindna.anerti.exception.http.UnauthorizedException;
import com.techindna.anerti.file.bucket.BucketComponent;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.CourseRepository;
import com.techindna.anerti.repository.ExamRepository;
import com.techindna.anerti.repository.ClassRepository;
import com.techindna.anerti.repository.GradeRepository;
import com.techindna.anerti.repository.ReportRepository;
import com.techindna.anerti.repository.StudentInheritanceRepository;
import com.techindna.anerti.repository.enums.UserRole;
import com.techindna.anerti.repository.model.JCourse;
import com.techindna.anerti.repository.model.JExam;
import com.techindna.anerti.repository.model.JGrade;
import com.techindna.anerti.repository.model.JStudentInheritance;
import com.techindna.anerti.repository.model.JClass;
import com.techindna.anerti.repository.model.JUser;
import com.techindna.anerti.validator.DataValidator;
import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class ReportService {

  private final ReportRepository reportRepository;
  private final ClassRepository classRepository;
  private final GradeRepository gradeRepository;
  private final StudentInheritanceRepository studentInheritanceRepository;
  private final AuthRepository authRepository;
  private final DataValidator dataValidator;
  private final EventProducer<GradeReportRequested> eventProducer;
  private final ExamRepository examRepository;
  private final CourseRepository courseRepository;
  private final BucketComponent bucketComponent;
  private final PdfGenerator pdfGenerator;

  @Transactional(readOnly = true)
  public ClassRankingResponse getClassRanking(UUID classId) {
    JClass clazz =
        classRepository
            .findById(classId)
            .orElseThrow(() -> new NotFoundException("Class %s not found".formatted(classId)));

    List<Object[]> rows = reportRepository.findClassRanking(classId);

    List<ClassRankingEntry> ranking =
        rows.stream()
            .map(
                row ->
                    new ClassRankingEntry(
                        ((Number) row[0]).intValue(),
                        UUID.fromString(row[1].toString()),
                        (String) row[2],
                        (String) row[3],
                        (String) row[4],
                        new BigDecimal(row[5].toString()).setScale(2)))
            .toList();

    return new ClassRankingResponse(clazz.getId(), clazz.getName(), clazz.getYearOf(), ranking);
  }

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

  @Transactional
  public GradeReportResponse requestGradeReport(GradeReportInput request) {
    UUID studentInheritanceId = request.studentInheritanceId();
    String academicYear = request.academicYear();

    dataValidator.checkNullData(
        "studentInheritanceId",
        studentInheritanceId != null ? studentInheritanceId.toString() : null);
    dataValidator.validateAcademicYear(academicYear);

    JStudentInheritance student =
        studentInheritanceRepository
            .findById(studentInheritanceId)
            .orElseThrow(
                () ->
                    new NotFoundException("Student %s not found".formatted(studentInheritanceId)));

    if (student.getClassId() == null) {
      throw new ConflictException(
          "Student %s is not enrolled in a class".formatted(studentInheritanceId));
    }

    long gradeCount =
        gradeRepository.countByStudentAndAcademicYear(studentInheritanceId, academicYear);
    if (gradeCount == 0) {
      throw new ConflictException(
          "Student %s has no grades for academic year %s"
              .formatted(studentInheritanceId, academicYear));
    }

    JUser user = currentUser();

    var event =
        GradeReportRequested.builder()
            .studentInheritanceId(studentInheritanceId)
            .academicYear(academicYear)
            .studentEmail(user.getEmail())
            .build();

    eventProducer.accept(List.of(event));

    return new GradeReportResponse(
        "Grade report for academic year %s will be sent to your email shortly"
            .formatted(academicYear),
        studentInheritanceId,
        academicYear);
  }

  @Transactional(readOnly = true)
  @SneakyThrows
  public ReportResult generateAndUpload(UUID studentInheritanceId, String academicYear) {
    JStudentInheritance student =
        studentInheritanceRepository
            .findById(studentInheritanceId)
            .orElseThrow(
                () ->
                    new NotFoundException("Student %s not found".formatted(studentInheritanceId)));

    JUser user =
        authRepository
            .findByStudentInheritanceId(studentInheritanceId)
            .orElseThrow(() -> new NotFoundException("User not found for student"));

    var grades =
        gradeRepository
            .search(studentInheritanceId, null, academicYear, null, PageRequest.of(0, 1000))
            .getContent();

    List<PdfGenerator.GradeRow> rows = grades.stream().map(this::toGradeRow).toList();

    String studentName = user.getFirstName() + " " + user.getLastName();
    byte[] pdf =
        pdfGenerator.generateGradeReport(studentName, student.getRef(), academicYear, rows);

    String bucketKey =
        "grade-reports/%s/%s.pdf".formatted(studentInheritanceId, Instant.now().toEpochMilli());
    File tempFile = File.createTempFile("grade-report-", ".pdf");
    Files.write(tempFile.toPath(), pdf);
    bucketComponent.upload(tempFile, bucketKey);
    tempFile.delete();

    URL presignedUrl = bucketComponent.presign(bucketKey, Duration.ofHours(24));

    return new ReportResult(presignedUrl.toString(), academicYear);
  }

  public record ReportResult(String downloadUrl, String academicYear) {}

  private PdfGenerator.GradeRow toGradeRow(JGrade g) {
    JExam exam = examRepository.findById(g.getExamId()).orElse(null);
    String courseRef = "";
    String coefficient = "";
    if (exam != null) {
      JCourse course = courseRepository.findById(exam.getCourseId()).orElse(null);
      if (course != null) {
        courseRef = course.getRef();
      }
      coefficient = exam.getCoefficient().toPlainString();
    }
    return new PdfGenerator.GradeRow(
        courseRef, "", formatInstant(g.getCreatedAt()), g.getValue().toPlainString(), coefficient);
  }

  private String formatInstant(Instant instant) {
    return instant == null
        ? ""
        : DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneOffset.UTC).format(instant);
  }

  private JUser currentUser() {
    return authRepository
        .findById(
            UUID.fromString(
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal()))
        .orElseThrow(() -> new UnauthorizedException("Authentication required."));
  }
}
