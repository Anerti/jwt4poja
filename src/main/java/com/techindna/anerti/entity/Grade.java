package com.techindna.anerti.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Grade(UUID id, UUID studentId, UUID examId, BigDecimal grade, Instant createdAt) {}
