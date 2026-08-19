package com.techindna.anerti.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ClassRankingEntry(
    int rank,
    UUID studentId,
    String firstName,
    String lastName,
    String ref,
    BigDecimal generalAverage) {}
