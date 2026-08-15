package com.techindna.anerti.dto;

import java.time.Instant;
import java.util.UUID;

public record TeacherCourse(UUID id, UUID teacherId, UUID courseId, Instant assignedAt) {}
