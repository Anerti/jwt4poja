package com.techindna.anerti.endpoint.rest.controller;

import com.techindna.anerti.dto.CreateGradeInput;
import com.techindna.anerti.dto.GradeOutput;
import com.techindna.anerti.service.GradeService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/grades")
@AllArgsConstructor
public class GradeController {

  private final GradeService gradeService;

  @PostMapping
  public ResponseEntity<GradeOutput> createGrade(@RequestBody CreateGradeInput request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(gradeService.createGrade(request));
  }
}
