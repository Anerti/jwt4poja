package com.techindna.anerti.mapper;

import com.techindna.anerti.dto.RegisterInput;
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
