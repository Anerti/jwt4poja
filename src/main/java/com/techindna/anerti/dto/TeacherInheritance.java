package com.techindna.anerti.dto;

import com.techindna.anerti.repository.enums.TeacherStatus;
import java.time.Instant;
import java.util.UUID;

public record TeacherInheritance(
    UUID id, String ref, Instant joinedAt, TeacherStatus teacherStatus) {}
