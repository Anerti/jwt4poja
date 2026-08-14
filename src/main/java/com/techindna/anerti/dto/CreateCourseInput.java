package com.techindna.anerti.dto;

import com.techindna.anerti.repository.enums.CourseType;

public record CreateCourseInput(String ref, String title, CourseType type, Integer credits) {}
