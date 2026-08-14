package com.techindna.anerti.dto;

import com.techindna.anerti.repository.enums.LearningPath;
import com.techindna.anerti.repository.enums.Level;
import java.time.Instant;

public record CreateStudentInput(
    String username,
    String password,
    String firstName,
    String lastName,
    String email,
    String ref,
    Instant joinedAt,
    Level level,
    LearningPath learningPath,
    String className) {}
