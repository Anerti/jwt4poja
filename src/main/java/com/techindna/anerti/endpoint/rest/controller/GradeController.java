package com.techindna.anerti.endpoint.rest.controller;

import com.techindna.anerti.dto.CreateGradeInput;
import com.techindna.anerti.dto.GradeListResponse;
import com.techindna.anerti.dto.GradeOutput;
import com.techindna.anerti.dto.HistoryListResponse;
import com.techindna.anerti.dto.UpdateGradeInput;
import com.techindna.anerti.service.GradeService;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class GradeController {

  private final GradeService gradeService;

  @PostMapping("/grades")
  public ResponseEntity<GradeOutput> createGrade(@RequestBody CreateGradeInput request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(gradeService.createGrade(request));
  }

  @GetMapping("/grades/{studentId}")
  public ResponseEntity<GradeListResponse> listGrades(
      @PathVariable UUID studentId,
      @RequestParam(required = false) UUID examId,
      @RequestParam(required = false) Integer academicYear,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size) {
    return ResponseEntity.ok(gradeService.listGrades(studentId, examId, academicYear, page, size));
  }

  @PatchMapping("/grades/{gradeId}")
  public ResponseEntity<GradeOutput> updateGrade(
      @PathVariable UUID gradeId, @RequestBody UpdateGradeInput request) {
    return ResponseEntity.ok(gradeService.updateGrade(gradeId, request));
  }

  @GetMapping("/grades/{gradeId}/history")
  public ResponseEntity<HistoryListResponse> getGradeHistory(@PathVariable UUID gradeId) {
    return ResponseEntity.ok(gradeService.getGradeHistory(gradeId));
  }
}
