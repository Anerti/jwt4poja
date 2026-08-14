package com.techindna.anerti.endpoint.rest.controller;

import com.techindna.anerti.dto.CreateStudentGroupListResponse;
import com.techindna.anerti.dto.CreateStudentGroupRequest;
import com.techindna.anerti.service.StudentGroupService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/student-groups")
@AllArgsConstructor
public class StudentGroupController {

  private final StudentGroupService studentGroupService;

  @PostMapping
  public ResponseEntity<CreateStudentGroupListResponse> enrollStudents(
      @RequestBody CreateStudentGroupRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(studentGroupService.enrollStudents(request));
  }
}
