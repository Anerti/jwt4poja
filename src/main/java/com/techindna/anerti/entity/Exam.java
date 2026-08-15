package com.techindna.anerti.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Exam(
    UUID id,
    UUID courseId,
    BigDecimal coefficient,
    Integer academicYear,
    Instant date,
    Instant createdAt,
    Instant updatedAt) {}
