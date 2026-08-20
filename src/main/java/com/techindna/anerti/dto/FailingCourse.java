package com.techindna.anerti.dto;

import java.math.BigDecimal;

public record FailingCourse(String courseRef, String courseTitle, BigDecimal weightedAverage) {}
