package com.techindna.anerti.mapper;

import com.techindna.anerti.dto.CreateTeacherCourseInput;
import com.techindna.anerti.dto.TeacherCourse;
import com.techindna.anerti.repository.model.JTeacherCourse;
import org.springframework.stereotype.Component;

@Component
public class TeacherCourseMapper {

  public JTeacherCourse toRepository(CreateTeacherCourseInput request) {
    return JTeacherCourse.builder()
        .teacherInheritanceId(request.teacherId())
        .courseId(request.courseId())
        .assignedAt(request.assignedAt())
        .build();
  }

  public TeacherCourse toDto(JTeacherCourse jTeacherCourse) {
    return new TeacherCourse(
        jTeacherCourse.getId(),
        jTeacherCourse.getTeacherInheritanceId(),
        jTeacherCourse.getCourseId(),
        jTeacherCourse.getAssignedAt());
  }
}
