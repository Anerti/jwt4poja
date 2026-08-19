package com.techindna.anerti.dto;

import java.time.Instant;
import java.util.UUID;

public record ClassOutput(
    UUID id, String name, Integer yearOf, Instant createdAt, Instant updatedAt) {}
