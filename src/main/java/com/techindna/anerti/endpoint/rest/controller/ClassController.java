package com.techindna.anerti.endpoint.rest.controller;

import com.techindna.anerti.dto.ClassOutput;
import com.techindna.anerti.dto.CreateClassInput;
import com.techindna.anerti.service.ClassService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/classes")
@AllArgsConstructor
public class ClassController {

  private final ClassService classService;

  @PostMapping
  public ResponseEntity<ClassOutput> createClass(@RequestBody CreateClassInput request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(classService.createClass(request));
  }
}
