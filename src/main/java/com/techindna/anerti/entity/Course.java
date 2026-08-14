package com.techindna.anerti.entity;

import com.techindna.anerti.repository.enums.CourseType;
import java.time.Instant;
import java.util.UUID;

public record Course(
    UUID id,
    String ref,
    String title,
    CourseType type,
    Integer credits,
    Instant createdAt,
    Instant updatedAt) {}
