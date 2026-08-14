package com.techindna.anerti.mapper;

import com.techindna.anerti.dto.CourseOutput;
import com.techindna.anerti.dto.CreateCourseInput;
import com.techindna.anerti.entity.Course;
import com.techindna.anerti.repository.enums.CourseType;
import com.techindna.anerti.repository.model.JCourse;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {

  public JCourse toRepository(CreateCourseInput request) {
    return JCourse.builder()
        .ref(request.ref().strip())
        .title(request.title().strip())
        .type(request.type() != null ? request.type() : CourseType.COMMON)
        .credits(request.credits())
        .build();
  }

  public Course toEntity(JCourse jCourse) {
    return new Course(
        jCourse.getId(),
        jCourse.getRef(),
        jCourse.getTitle(),
        jCourse.getType(),
        jCourse.getCredits(),
        jCourse.getCreatedAt(),
        jCourse.getUpdatedAt());
  }

  public CourseOutput toDto(Course course) {
    return new CourseOutput(
        course.id(),
        course.ref(),
        course.title(),
        course.type(),
        course.credits(),
        course.createdAt(),
        course.updatedAt());
  }
}
