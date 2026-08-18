package com.techindna.anerti.endpoint.rest.controller;

import com.techindna.anerti.dto.GradeReportOutput;
import com.techindna.anerti.dto.GradeReportRequest;
import com.techindna.anerti.endpoint.event.EventProducer;
import com.techindna.anerti.endpoint.event.model.GradeReportRequested;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.model.JUser;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/grade-reports")
@AllArgsConstructor
public class GradeReportController {

  private final EventProducer<GradeReportRequested> eventProducer;
  private final AuthRepository authRepository;

  @PostMapping
  public ResponseEntity<GradeReportOutput> requestGradeReport(
      @RequestBody GradeReportRequest request) {

    String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    JUser user =
        authRepository
            .findById(UUID.fromString(userId))
            .orElseThrow(() -> new RuntimeException("User not found"));

    UUID studentId = request.studentInheritanceId();
    String email = user.getEmail();

    var event =
        GradeReportRequested.builder().studentInheritanceId(studentId).studentEmail(email).build();

    eventProducer.accept(List.of(event));

    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(new GradeReportOutput("Grade report generation started", null));
  }
}
