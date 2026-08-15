package com.techindna.anerti.endpoint.rest.controller;

import com.techindna.anerti.dto.CreateTeacherCourseInput;
import com.techindna.anerti.dto.TeacherCourse;
import com.techindna.anerti.service.TeacherCourseService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/teacher-courses")
@AllArgsConstructor
public class TeacherCourseController {

  private final TeacherCourseService teacherCourseService;

  @PostMapping
  public ResponseEntity<TeacherCourse> createTeacherCourse(
      @RequestBody CreateTeacherCourseInput request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(teacherCourseService.createTeacherCourse(request));
  }
}
