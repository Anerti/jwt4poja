package com.techindna.anerti.dto;

import com.techindna.anerti.repository.enums.UserRole;
import java.time.Instant;
import java.util.UUID;

public record UserExtendStudent(
    UUID id,
    String username,
    String firstName,
    String lastName,
    String email,
    UserRole role,
    Instant createdAt,
    Instant updatedAt,
    StudentInheritance studentInheritance,
    UUID groupId,
    UUID classId) {}
