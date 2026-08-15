package com.techindna.anerti.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateExamInput(
    UUID courseId, BigDecimal coefficient, Integer academicYear, Instant date) {}
