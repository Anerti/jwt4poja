package com.techindna.anerti.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record HistoryEntry(UUID id, BigDecimal grade, String description, Instant createdAt) {}
