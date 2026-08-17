package com.techindna.anerti.service.event;

import com.techindna.anerti.endpoint.event.EventProducer;
import com.techindna.anerti.endpoint.event.model.GradeReportRequested;
import com.techindna.anerti.endpoint.event.model.SendEmailRequested;
import com.techindna.anerti.service.GradeReportService;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class GradeReportRequestedService implements Consumer<GradeReportRequested> {

  private final GradeReportService gradeReportService;
  private final EventProducer<SendEmailRequested> emailProducer;

  @Override
  public void accept(GradeReportRequested event) {
    try {
      var result = gradeReportService.generateAndUpload(event.getStudentInheritanceId());

      String htmlBody =
          """
          <h1>Releve de notes</h1>
          <p>Votre releve de notes est disponible.</p>
          <p><a href="%s">Telecharger votre releve</a></p>
          <p>Ce lien expire dans 24 heures.</p>
          """
              .formatted(result.downloadUrl());

      var emailEvent =
          SendEmailRequested.builder()
              .to(event.getStudentEmail())
              .subject("Votre releve de notes - HEI")
              .htmlBody(htmlBody)
              .build();

      emailProducer.accept(List.of(emailEvent));
    } catch (Exception e) {
      log.error(
          "Failed to generate grade report for student {}", event.getStudentInheritanceId(), e);
    }
  }
}
