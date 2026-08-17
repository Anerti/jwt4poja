package com.techindna.anerti.endpoint.rest.controller;

import com.techindna.anerti.dto.CreateGradeInput;
import com.techindna.anerti.dto.GradeListResponse;
import com.techindna.anerti.dto.GradeOutput;
import com.techindna.anerti.service.GradeService;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/grades")
@AllArgsConstructor
public class GradeController {

  private final GradeService gradeService;

  @GetMapping("/{studentInheritanceId}")
  public ResponseEntity<GradeListResponse> listGrades(
      @PathVariable UUID studentInheritanceId,
      @RequestParam(required = false) String courseRef,
      @RequestParam(required = false) String academicYear,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(gradeService.listGrades(studentInheritanceId, courseRef, academicYear, page, size));
  }

  @PostMapping
  public ResponseEntity<GradeOutput> createGrade(@RequestBody CreateGradeInput request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(gradeService.createGrade(request));
  }
}
