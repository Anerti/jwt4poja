package com.techindna.anerti.endpoint.rest.controller;

import com.techindna.anerti.dto.StudentGeneralAverage;
import com.techindna.anerti.service.ReportService;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
@AllArgsConstructor
public class ReportController {

  private final ReportService reportService;

  @GetMapping("/students/{studentInheritanceId}/average")
  public ResponseEntity<StudentGeneralAverage> getStudentAverage(
      @PathVariable UUID studentInheritanceId,
      @RequestParam(required = false) String academicYear) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(reportService.getStudentAverage(studentInheritanceId, academicYear));
  }
}
