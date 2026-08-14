package com.techindna.anerti.dto;

import com.techindna.anerti.repository.enums.CourseType;

public record CreateGroupInput(String ref, CourseType type) {}
