package com.techindna.anerti.dto;

import com.techindna.anerti.repository.enums.CourseType;
import java.time.Instant;
import java.util.UUID;

public record CourseOutput(
    UUID id,
    String ref,
    String title,
    CourseType type,
    Integer credits,
    Instant createdAt,
    Instant updatedAt) {}
