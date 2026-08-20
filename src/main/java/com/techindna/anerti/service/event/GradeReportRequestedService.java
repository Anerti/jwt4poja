package com.techindna.anerti.service.event;

import com.techindna.anerti.endpoint.event.model.GradeReportRequested;
import com.techindna.anerti.file.bucket.BucketComponent;
import com.techindna.anerti.mail.Email;
import com.techindna.anerti.mail.Mailer;
import com.techindna.anerti.repository.GradeRepository;
import com.techindna.anerti.repository.ReportRepository;
import com.techindna.anerti.repository.StudentInheritanceRepository;
import com.techindna.anerti.repository.UserRepository;
import com.techindna.anerti.repository.model.JStudentInheritance;
import com.techindna.anerti.repository.model.JUser;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.mail.internet.InternetAddress;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class GradeReportRequestedService implements Consumer<GradeReportRequested> {

  private final Mailer mailer;
  private final BucketComponent bucketComponent;
  private final StudentInheritanceRepository studentInheritanceRepository;
  private final UserRepository userRepository;
  private final GradeRepository gradeRepository;
  private final ReportRepository reportRepository;

  @SneakyThrows
  @Override
  public void accept(GradeReportRequested event) {
    JStudentInheritance student =
        studentInheritanceRepository
            .findById(event.getStudentInheritanceId())
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "Student inheritance %s not found"
                            .formatted(event.getStudentInheritanceId())));

    JUser user =
        userRepository
            .findByStudentInheritanceId(student.getId())
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "User for student inheritance %s not found".formatted(student.getId())));

    List<Object[]> courses =
        gradeRepository.findCoursesForStudentInYear(student.getId(), event.getAcademicYear());

    BigDecimal generalAverage =
        gradeRepository.computeGeneralAverage(student.getId(), event.getAcademicYear(), null);

    String rankInfo = "";
    int totalStudents = 0;
    if (student.getClassId() != null) {
      List<Object[]> ranking = reportRepository.findClassRanking(student.getClassId());
      totalStudents = ranking.size();
      for (Object[] row : ranking) {
        UUID rowStudentId = (UUID) row[1];
        if (rowStudentId.equals(student.getId())) {
          rankInfo = "%d / %d".formatted(((Number) row[0]).intValue(), totalStudents);
          break;
        }
      }
    }

    byte[] pdfBytes = buildPdf(student, user, event.getAcademicYear(), courses, generalAverage, rankInfo);
    File tempFile = File.createTempFile("grade-report-", ".pdf");
    try {
      Files.write(tempFile.toPath(), pdfBytes);
      String s3Key =
          "grade-reports/%s/%s.pdf".formatted(student.getId(), event.getAcademicYear());
      bucketComponent.upload(tempFile, s3Key);
    } finally {
      Files.deleteIfExists(tempFile.toPath());
    }

    String subject = "Grade Report - %s".formatted(event.getAcademicYear());
    String htmlBody =
        buildHtmlBody(user, event.getAcademicYear(), generalAverage, rankInfo);

    File emailAttachment = File.createTempFile("grade-report-email-", ".pdf");
    try {
      Files.write(emailAttachment.toPath(), pdfBytes);
      InternetAddress recipientAddress = new InternetAddress(event.getRecipientEmail());
      mailer.accept(
          new Email(
              recipientAddress,
              List.of(),
              List.of(),
              subject,
              htmlBody,
              List.of(emailAttachment)));
    } finally {
      Files.deleteIfExists(emailAttachment.toPath());
    }
  }

  private byte[] buildPdf(
      JStudentInheritance student,
      JUser user,
      String academicYear,
      List<Object[]> courses,
      BigDecimal generalAverage,
      String rankInfo)
      throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Document document = new Document(PageSize.A4, 50, 50, 50, 50);
    PdfWriter.getInstance(document, baos);
    document.open();

    Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

    Paragraph title = new Paragraph("Grade Report", titleFont);
    title.setAlignment(Element.ALIGN_CENTER);
    document.add(title);
    document.add(new Paragraph(" "));

    document.add(new Paragraph("Academic Year: " + academicYear, headerFont));
    document.add(new Paragraph("Student: " + user.getFirstName() + " " + user.getLastName(), normalFont));
    document.add(new Paragraph("Reference: " + student.getRef(), normalFont));
    document.add(new Paragraph(" "));

    if (!courses.isEmpty()) {
      document.add(new Paragraph("Per-Course Breakdown", headerFont));
      PdfPTable table = new PdfPTable(3);
      table.setWidthPercentage(100);
      table.setWidths(new float[]{50, 25, 25});
      table.addCell("Course");
      table.addCell("Credits");
      table.addCell("Weighted Average");

      for (Object[] row : courses) {
        String ref = (String) row[0];
        String title2 = (String) row[1];
        int credits = ((Number) row[2]).intValue();
        BigDecimal avg =
            gradeRepository.computeCourseGrade(student.getId(), ref);
        table.addCell(ref + " - " + title2);
        table.addCell(String.valueOf(credits));
        table.addCell(avg != null ? avg.setScale(2, RoundingMode.HALF_UP).toString() : "N/A");
      }
      document.add(table);
      document.add(new Paragraph(" "));
    }

    document.add(new Paragraph("General Average: " + generalAverage.setScale(2, RoundingMode.HALF_UP), headerFont));
    if (!rankInfo.isEmpty()) {
      document.add(new Paragraph("Class Ranking: " + rankInfo, headerFont));
    }

    document.close();
    return baos.toByteArray();
  }

  private String buildHtmlBody(
      JUser user, String academicYear, BigDecimal generalAverage, String rankInfo) {
    StringBuilder html = new StringBuilder();
    html.append("<html><body>");
    html.append("<h2>Grade Report - ").append(academicYear).append("</h2>");
    html.append("<p>Hello ").append(user.getFirstName()).append(",</p>");
    html.append(
        "<p>Your grade report for academic year "
            + academicYear
            + " has been generated and is attached to this email.</p>");
    html.append(
        "<p><strong>General Average:</strong> "
            + generalAverage.setScale(2, RoundingMode.HALF_UP)
            + "</p>");
    if (!rankInfo.isEmpty()) {
      html.append("<p><strong>Class Ranking:</strong> ").append(rankInfo).append("</p>");
    }
    html.append("</body></html>");
    return html.toString();
  }
}
