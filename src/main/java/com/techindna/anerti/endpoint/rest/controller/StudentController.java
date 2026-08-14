package com.techindna.anerti.endpoint.rest.controller;

import com.techindna.anerti.dto.CreateStudentInput;
import com.techindna.anerti.dto.UserExtendStudent;
import com.techindna.anerti.service.StudentService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/students")
@AllArgsConstructor
public class StudentController {

  private final StudentService studentService;

  @PostMapping
  public ResponseEntity<UserExtendStudent> createStudent(@RequestBody CreateStudentInput request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(studentService.createStudent(request));
  }
}
