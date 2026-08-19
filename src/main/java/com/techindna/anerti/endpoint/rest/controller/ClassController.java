package com.techindna.anerti.endpoint.rest.controller;

import com.techindna.anerti.dto.ClassListResponse;
import com.techindna.anerti.dto.ClassOutput;
import com.techindna.anerti.dto.CreateClassInput;
import com.techindna.anerti.service.ClassService;
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
@RequestMapping("/classes")
@AllArgsConstructor
public class ClassController {

  private final ClassService classService;

  @GetMapping
  public ResponseEntity<ClassListResponse> listClasses(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Integer yearOf,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(classService.listClasses(search, yearOf, page, size));
  }

  @PostMapping
  public ResponseEntity<ClassOutput> createClass(@RequestBody CreateClassInput request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(classService.createClass(request));
  }
}
