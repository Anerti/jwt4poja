package com.techindna.anerti.endpoint.rest.controller;

import com.techindna.anerti.dto.GradeReportInput;
import com.techindna.anerti.dto.GradeReportResponse;
import com.techindna.anerti.service.GradeReportService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/grade-reports")
@AllArgsConstructor
public class GradeReportController {

  private final GradeReportService gradeReportService;

  @PostMapping
  public ResponseEntity<GradeReportResponse> requestGradeReport(
      @RequestBody GradeReportInput request) {
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(gradeReportService.requestGradeReport(request));
  }
}
