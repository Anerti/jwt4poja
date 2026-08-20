package com.techindna.anerti.dto;

import java.util.List;
import java.util.UUID;

public record EnrollmentRejected(
    UUID studentId, String message, List<FailingCourse> failingCourses) {}
