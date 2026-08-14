package com.techindna.anerti.dto;

import java.time.Instant;
import java.util.UUID;

public record CreateStudentGroupOutput(
    UUID studentId, UUID groupId, Instant joinedAt, Instant leftAt) {}
