package com.techindna.anerti.endpoint.rest.controller;

import com.techindna.anerti.dto.GraduationListResponse;
import com.techindna.anerti.service.GraduationService;
import java.util.UUID;
import lombok.AllArgsConstructor;
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

  @GetMapping("/{classId}")
  public ResponseEntity<GraduationListResponse> listGraduates(@PathVariable UUID classId) {
    return ResponseEntity.ok(graduationService.listGraduates(classId));
  }
}
