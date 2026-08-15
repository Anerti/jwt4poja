package com.techindna.anerti.service;

import com.techindna.anerti.exception.http.NotFoundException;
import com.techindna.anerti.repository.UserRepository;
import com.techindna.anerti.repository.model.JUser;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UserContext {

  private final UserRepository userRepository;

  public JUser currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String userId = (String) authentication.getPrincipal();
    return userRepository
        .findById(UUID.fromString(userId))
        .orElseThrow(() -> new NotFoundException("User %s not found".formatted(userId)));
  }
}
