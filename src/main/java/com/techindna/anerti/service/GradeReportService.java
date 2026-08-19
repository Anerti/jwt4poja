package com.techindna.anerti.service;

import com.techindna.anerti.dto.GradeReportInput;
import com.techindna.anerti.dto.GradeReportResponse;
import com.techindna.anerti.endpoint.event.EventProducer;
import com.techindna.anerti.endpoint.event.model.GradeReportRequested;
import com.techindna.anerti.exception.http.ConflictException;
import com.techindna.anerti.exception.http.NotFoundException;
import com.techindna.anerti.exception.http.UnauthorizedException;
import com.techindna.anerti.file.bucket.BucketComponent;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.CourseRepository;
import com.techindna.anerti.repository.ExamRepository;
import com.techindna.anerti.repository.GradeRepository;
import com.techindna.anerti.repository.StudentInheritanceRepository;
import com.techindna.anerti.repository.model.JCourse;
import com.techindna.anerti.repository.model.JExam;
import com.techindna.anerti.repository.model.JGrade;
import com.techindna.anerti.repository.model.JStudentInheritance;
import com.techindna.anerti.repository.model.JUser;
import com.techindna.anerti.validator.DataValidator;
import java.io.File;
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
public class GradeReportService {

  private final StudentInheritanceRepository studentInheritanceRepository;
  private final AuthRepository authRepository;
  private final GradeRepository gradeRepository;
  private final ExamRepository examRepository;
  private final CourseRepository courseRepository;
  private final BucketComponent bucketComponent;
  private final PdfGenerator pdfGenerator;
  private final DataValidator dataValidator;
  private final EventProducer<GradeReportRequested> eventProducer;

  @Transactional
  public GradeReportResponse requestGradeReport(GradeReportInput request) {
    dataValidator.checkNullData("studentInheritanceId", request.studentInheritanceId().toString());
    dataValidator.validateAcademicYear(request.academicYear());

    UUID studentInheritanceId = request.studentInheritanceId();
    String academicYear = request.academicYear();

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

    java.net.URL presignedUrl = bucketComponent.presign(bucketKey, Duration.ofHours(24));

    return new ReportResult(presignedUrl.toString(), academicYear);
  }

  public record ReportResult(String downloadUrl, String academicYear) {}

  private JUser currentUser() {
    return authRepository
        .findById(
            UUID.fromString(
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal()))
        .orElseThrow(() -> new UnauthorizedException("Authentication required."));
  }

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
}
