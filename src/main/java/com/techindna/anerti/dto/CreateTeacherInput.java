package com.techindna.anerti.dto;

import com.techindna.anerti.repository.enums.TeacherStatus;
import java.time.Instant;

public record CreateTeacherInput(
    String username,
    String password,
    String firstName,
    String lastName,
    String email,
    String ref,
    Instant joinedAt,
    TeacherStatus teacherStatus) {}
