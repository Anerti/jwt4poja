package com.techindna.anerti.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CourseGradeOutput(
    UUID studentInheritanceId, String courseRef, BigDecimal weightedAverage) {}
