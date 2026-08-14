package com.techindna.anerti.endpoint.rest.controller;

import com.techindna.anerti.dto.CreateGroupInput;
import com.techindna.anerti.dto.GroupOutput;
import com.techindna.anerti.service.GroupService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/groups")
@AllArgsConstructor
public class GroupController {

  private final GroupService groupService;

  @PostMapping
  public ResponseEntity<GroupOutput> createGroup(@RequestBody CreateGroupInput request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(groupService.createGroup(request));
  }
}
