package com.techindna.anerti.endpoint.rest.controller;

import com.techindna.anerti.dto.CourseOutput;
import com.techindna.anerti.dto.CreateCourseInput;
import com.techindna.anerti.service.CourseService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/courses")
@AllArgsConstructor
public class CourseController {

  private final CourseService courseService;

  @PostMapping
  public ResponseEntity<CourseOutput> createCourse(@RequestBody CreateCourseInput request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(courseService.createCourse(request));
  }
}
