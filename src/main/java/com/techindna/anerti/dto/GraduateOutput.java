package com.techindna.anerti.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record GraduateOutput(
    int rank, UUID studentId, String firstName, String lastName, BigDecimal average) {}
