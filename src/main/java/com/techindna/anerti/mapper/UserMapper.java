package com.techindna.anerti.mapper;

import com.techindna.anerti.entity.User;
import com.techindna.anerti.repository.model.JUser;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

  public User toDomain(JUser jUser) {
    return new User(
        jUser.getId(),
        jUser.getUsername(),
        jUser.getFirstName(),
        jUser.getLastName(),
        jUser.getEmail(),
        jUser.getRole(),
        jUser.getCreatedAt(),
        jUser.getUpdatedAt());
  }
}
