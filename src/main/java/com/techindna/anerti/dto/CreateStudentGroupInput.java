package com.techindna.anerti.dto;

import java.time.Instant;
import java.util.UUID;

public record CreateStudentGroupInput(UUID studentId, UUID groupId, Instant joinedAt) {}
