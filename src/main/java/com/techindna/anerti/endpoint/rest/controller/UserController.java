package com.techindna.anerti.endpoint.rest.controller;

import com.techindna.anerti.dto.UserListResponse;
import com.techindna.anerti.entity.enums.SortDirection;
import com.techindna.anerti.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@AllArgsConstructor
public class UserController {

  private final UserService userService;

  @GetMapping
  public ResponseEntity<UserListResponse> listUsers(
      @RequestParam(required = false) String search,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "ASC") SortDirection sort) {
    return ResponseEntity.ok(userService.listUsers(search, page, size, sort));
  }
}
