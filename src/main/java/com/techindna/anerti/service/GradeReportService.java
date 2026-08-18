package com.techindna.anerti.service;

import com.techindna.anerti.dto.GradeReportOutput;
import com.techindna.anerti.exception.http.NotFoundException;
import com.techindna.anerti.file.bucket.BucketComponent;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.GradeRepository;
import com.techindna.anerti.repository.StudentInheritanceRepository;
import com.techindna.anerti.repository.model.JGrade;
import com.techindna.anerti.repository.model.JStudentInheritance;
import com.techindna.anerti.repository.model.JUser;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class GradeReportService {

  private final StudentInheritanceRepository studentInheritanceRepository;
  private final AuthRepository authRepository;
  private final GradeRepository gradeRepository;
  private final BucketComponent bucketComponent;
  private final PdfGenerator pdfGenerator;

  @Transactional(readOnly = true)
  @SneakyThrows
  public GradeReportOutput generateAndUpload(UUID studentInheritanceId) {
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
            .search(studentInheritanceId, null, null, null, PageRequest.of(0, 1000))
            .getContent();

    List<PdfGenerator.GradeRow> rows = grades.stream().map(this::toGradeRow).toList();

    String studentName = user.getFirstName() + " " + user.getLastName();
    byte[] pdf = pdfGenerator.generateGradeReport(studentName, student.getRef(), rows);

    String bucketKey =
        "grade-reports/%s/%s.pdf".formatted(studentInheritanceId, Instant.now().toEpochMilli());
    File tempFile = File.createTempFile("grade-report-", ".pdf");
    Files.write(tempFile.toPath(), pdf);
    bucketComponent.upload(tempFile, bucketKey);
    tempFile.delete();

    java.net.URL presignedUrl = bucketComponent.presign(bucketKey, Duration.ofHours(24));

    return new GradeReportOutput("Grade report generated successfully", presignedUrl.toString());
  }

  private PdfGenerator.GradeRow toGradeRow(JGrade g) {
    return new PdfGenerator.GradeRow(
        g.getExamId().toString(),
        "",
        formatInstant(g.getCreatedAt()),
        g.getValue().toPlainString(),
        "");
  }

  private String formatInstant(Instant instant) {
    return instant == null
        ? ""
        : DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneOffset.UTC).format(instant);
  }
}
