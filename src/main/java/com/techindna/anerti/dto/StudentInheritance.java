package com.techindna.anerti.dto;

import com.techindna.anerti.repository.enums.LearningPath;
import com.techindna.anerti.repository.enums.Level;
import com.techindna.anerti.repository.enums.StudentStatus;
import java.time.Instant;
import java.util.UUID;

public record StudentInheritance(
    UUID id,
    String ref,
    Instant joinedAt,
    Level level,
    LearningPath learningPath,
    StudentStatus studentStatus) {}
