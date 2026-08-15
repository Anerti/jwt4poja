package com.techindna.anerti.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateGradeInput(UUID studentId, UUID examId, BigDecimal grade, String description) {}
