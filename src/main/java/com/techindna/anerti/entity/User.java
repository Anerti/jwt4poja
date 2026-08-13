package com.techindna.anerti.entity;

import com.techindna.anerti.repository.enums.UserRole;
import java.time.Instant;
import java.util.UUID;

public record User(
    UUID id,
    String username,
    String firstName,
    String lastName,
    String email,
    UserRole role,
    Instant createdAt,
    Instant updatedAt) {}
