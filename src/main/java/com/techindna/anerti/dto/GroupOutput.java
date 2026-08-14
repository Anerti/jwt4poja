package com.techindna.anerti.dto;

import com.techindna.anerti.repository.enums.CourseType;
import java.time.Instant;
import java.util.UUID;

public record GroupOutput(UUID id, String ref, CourseType type, Instant createdAt) {}
