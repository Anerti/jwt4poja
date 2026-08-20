package com.techindna.anerti.endpoint.rest.controller;

import com.techindna.anerti.dto.ClassRankingResponse;
import com.techindna.anerti.dto.StudentGeneralAverage;
import com.techindna.anerti.service.ReportService;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

  @GetMapping("/classes/{classId}/ranking")
  public ResponseEntity<ClassRankingResponse> getClassRanking(@PathVariable UUID classId) {
    return ResponseEntity.ok(reportService.getClassRanking(classId));
  }

  @GetMapping("/classes/{classId}/ranking/download")
  public ResponseEntity<byte[]> downloadClassRanking(@PathVariable UUID classId) {
    byte[] xlsx = reportService.downloadClassRanking(classId);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=class-ranking-%s.xlsx".formatted(classId))
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(xlsx);
  }

  @GetMapping("/students/{studentInheritanceId}/average")
  public ResponseEntity<StudentGeneralAverage> getStudentAverage(
      @PathVariable UUID studentInheritanceId,
      @RequestParam(required = false) String academicYear) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(reportService.getStudentAverage(studentInheritanceId, academicYear));
  }
}
