package com.techindna.anerti.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateGradeInput(
    UUID studentInheritanceId, UUID examId, BigDecimal value, String description) {}
