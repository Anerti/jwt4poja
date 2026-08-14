package com.techindna.anerti.endpoint.rest.controller;

import com.techindna.anerti.dto.CreateTeacherInput;
import com.techindna.anerti.dto.UserExtendTeacher;
import com.techindna.anerti.service.TeacherService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/teachers")
@AllArgsConstructor
public class TeacherController {

  private final TeacherService teacherService;

  @PostMapping
  public ResponseEntity<UserExtendTeacher> createTeacher(@RequestBody CreateTeacherInput request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(teacherService.createTeacher(request));
  }
}
