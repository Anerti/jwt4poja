package com.techindna.anerti.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExamOutput(
    UUID id,
    UUID courseId,
    BigDecimal coefficient,
    Integer academicYear,
    Instant date,
    Instant createdAt,
    Instant updatedAt) {}
