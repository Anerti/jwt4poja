package com.techindna.anerti.endpoint.rest.controller;

import com.techindna.anerti.dto.GraduationListResponse;
import com.techindna.anerti.service.ExcelGenerator;
import com.techindna.anerti.service.GraduationService;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/graduations")
@AllArgsConstructor
public class GraduationController {

  private final GraduationService graduationService;
  private final ExcelGenerator excelGenerator;

  @GetMapping("/{classId}")
  public ResponseEntity<GraduationListResponse> listGraduates(@PathVariable UUID classId) {
    return ResponseEntity.ok(graduationService.listGraduates(classId));
  }

  @GetMapping("/{classId}/download")
  public ResponseEntity<byte[]> downloadExcel(@PathVariable UUID classId) {
    GraduationListResponse response = graduationService.listGraduates(classId);
    byte[] excel =
        excelGenerator.generateGraduationExcel(
            response.className(), response.yearOf(), response.graduates());

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(
        MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    headers.setContentDispositionFormData(
        "attachment", "graduates_%s_%d.xlsx".formatted(response.className(), response.yearOf()));
    return new ResponseEntity<>(excel, headers, HttpStatus.OK);
  }
}
