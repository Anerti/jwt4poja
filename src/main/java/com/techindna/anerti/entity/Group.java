package com.techindna.anerti.entity;

import com.techindna.anerti.repository.enums.CourseType;
import java.time.Instant;
import java.util.UUID;

public record Group(UUID id, String ref, CourseType type, Instant createdAt) {}
