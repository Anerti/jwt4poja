package com.techindna.anerti.dto;

import java.util.List;
import java.util.UUID;

public record ClassRankingResponse(
    UUID classId, String className, int yearOf, List<ClassRankingEntry> ranking) {}
