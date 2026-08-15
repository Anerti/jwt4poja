package com.techindna.anerti.endpoint.rest.controller;

import com.techindna.anerti.dto.CreateStudentInput;
import com.techindna.anerti.dto.StudentListResponse;
import com.techindna.anerti.dto.UserExtendStudent;
import com.techindna.anerti.repository.enums.LearningPath;
import com.techindna.anerti.repository.enums.Level;
import com.techindna.anerti.repository.enums.StudentStatus;
import com.techindna.anerti.service.StudentService;
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
@RequestMapping("/students")
@AllArgsConstructor
public class StudentController {

  private final StudentService studentService;

  @GetMapping
  public ResponseEntity<StudentListResponse> listStudents(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Level level,
      @RequestParam(required = false) LearningPath learningPath,
      @RequestParam(required = false) StudentStatus studentStatus,
      @RequestParam(required = false) String groupRef,
      @RequestParam(required = false) String className,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(
            studentService.listStudents(
                search, level, learningPath, studentStatus, groupRef, className, page, size));
  }

  @PostMapping
  public ResponseEntity<UserExtendStudent> createStudent(@RequestBody CreateStudentInput request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(studentService.createStudent(request));
  }
}
