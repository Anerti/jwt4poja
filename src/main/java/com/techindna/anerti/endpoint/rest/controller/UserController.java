package com.techindna.anerti.endpoint.rest.controller;

import com.techindna.anerti.dto.UserListResponse;
import com.techindna.anerti.entity.User;
import com.techindna.anerti.entity.enums.SortDirection;
import com.techindna.anerti.service.UserService;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    return ResponseEntity.status(HttpStatus.OK)
        .body(userService.listUsers(search, page, size, sort));
  }

  @GetMapping("/{userId}")
  public ResponseEntity<User> getUser(@PathVariable UUID userId) {
    return ResponseEntity.status(HttpStatus.OK).body(userService.getUser(userId));
  }
}
