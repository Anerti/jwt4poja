package com.techindna.anerti.mapper;

import com.techindna.anerti.dto.RegisterInput;
import com.techindna.anerti.dto.UpdateUserInput;
import com.techindna.anerti.entity.User;
import com.techindna.anerti.entity.enums.UserRole;
import com.techindna.anerti.repository.model.JUser;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

  public JUser toEntity(RegisterInput request, String encodedPassword) {
    return JUser.builder()
        .username(request.username().strip())
        .password(encodedPassword)
        .firstName(request.firstName().strip())
        .lastName(request.lastName().strip())
        .email(request.email().strip().toLowerCase())
        .verified(false)
        .role(UserRole.CUSTOMER)
        .build();
  }

  public void applyUpdate(JUser jUser, UpdateUserInput input) {
    if (input.username() != null) {
      jUser.setUsername(input.username().strip());
    }
    if (input.firstName() != null) {
      jUser.setFirstName(input.firstName().strip());
    }
    if (input.lastName() != null) {
      jUser.setLastName(input.lastName().strip());
    }
  }

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
