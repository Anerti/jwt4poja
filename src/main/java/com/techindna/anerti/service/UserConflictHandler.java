package com.techindna.anerti.service;

import com.techindna.anerti.exception.http.ConflictException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class UserConflictHandler {

  public void conflictFrom(
      DataIntegrityViolationException e, String username, String email, String ref) {
    String message = e.getMostSpecificCause().getMessage();
    if (message.contains("username")) {
      throw new ConflictException("Cannot use username %s".formatted(username.strip()));
    }
    if (message.contains("email")) {
      throw new ConflictException("cannot use email %s".formatted(email.strip().toLowerCase()));
    }
    if (message.contains("ref")) {
      throw new ConflictException("Cannot use ref %s".formatted(ref.strip()));
    }
  }
}
