package com.techindna.anerti.endpoint.rest.controller;

import com.techindna.anerti.dto.CreateExamInput;
import com.techindna.anerti.dto.ExamOutput;
import com.techindna.anerti.service.ExamService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/exams")
@AllArgsConstructor
public class ExamController {

  private final ExamService examService;

  @PostMapping
  public ResponseEntity<ExamOutput> createExam(@RequestBody CreateExamInput request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(examService.createExam(request));
  }
}
