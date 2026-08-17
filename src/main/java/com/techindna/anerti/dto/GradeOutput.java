package com.techindna.anerti.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record GradeOutput(
    UUID id,
    UUID studentInheritanceId,
    UUID examId,
    BigDecimal value,
    String description,
    Instant createdAt) {}
