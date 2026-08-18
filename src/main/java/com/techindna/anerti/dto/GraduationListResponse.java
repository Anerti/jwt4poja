package com.techindna.anerti.dto;

import java.util.List;

public record GraduationListResponse(
    String className, int yearOf, List<GraduateOutput> graduates, Meta meta) {}
