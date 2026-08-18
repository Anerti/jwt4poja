package com.techindna.anerti.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record StudentGeneralAverage(
    UUID studentInheritanceId, String academicYear, BigDecimal generalAverage) {}
