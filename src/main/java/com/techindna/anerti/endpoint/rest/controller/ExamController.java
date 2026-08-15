package com.techindna.anerti.endpoint.rest.controller;

import com.techindna.anerti.dto.CreateExamInput;
import com.techindna.anerti.dto.ExamListResponse;
import com.techindna.anerti.dto.ExamOutput;
import com.techindna.anerti.service.ExamService;
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/exams")
@AllArgsConstructor
public class ExamController {

  private final ExamService examService;

  @GetMapping
  public ResponseEntity<ExamListResponse> listExams(
      @RequestParam(required = false) String ref,
      @RequestParam(required = false) Integer academicYear,
      @RequestParam(required = false) Instant startDate,
      @RequestParam(required = false) Instant endDate,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size) {
    return ResponseEntity.ok(
        examService.listExams(ref, academicYear, startDate, endDate, page, size));
  }

  @PostMapping
  public ResponseEntity<ExamOutput> createExam(@RequestBody CreateExamInput request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(examService.createExam(request));
  }
}
