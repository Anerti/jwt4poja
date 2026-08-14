package com.techindna.anerti.endpoint.rest.controller;

import com.techindna.anerti.dto.CreateTeacherInput;
import com.techindna.anerti.dto.TeacherListResponse;
import com.techindna.anerti.dto.UserExtendTeacher;
import com.techindna.anerti.repository.enums.TeacherStatus;
import com.techindna.anerti.service.TeacherService;
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
@RequestMapping("/teachers")
@AllArgsConstructor
public class TeacherController {

  private final TeacherService teacherService;

  @GetMapping
  public ResponseEntity<TeacherListResponse> listTeachers(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) TeacherStatus teacherStatus,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size) {
    return ResponseEntity.ok(teacherService.listTeachers(search, teacherStatus, page, size));
  }

  @PostMapping
  public ResponseEntity<UserExtendTeacher> createTeacher(@RequestBody CreateTeacherInput request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(teacherService.createTeacher(request));
  }
}
