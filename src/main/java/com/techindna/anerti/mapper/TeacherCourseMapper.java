package com.techindna.anerti.mapper;

import com.techindna.anerti.dto.CreateTeacherCourseInput;
import com.techindna.anerti.dto.TeacherCourse;
import com.techindna.anerti.repository.model.JTeacherCourse;
import com.techindna.anerti.repository.model.JTeacherInheritance;
import org.springframework.stereotype.Component;

@Component
public class TeacherCourseMapper {

  public JTeacherCourse toRepository(
      CreateTeacherCourseInput request, JTeacherInheritance teacher) {
    return JTeacherCourse.builder()
        .teacherInheritance(teacher)
        .courseId(request.courseId())
        .assignedAt(request.assignedAt())
        .build();
  }

  public TeacherCourse toDto(JTeacherCourse jTeacherCourse) {
    return new TeacherCourse(
        jTeacherCourse.getId(),
        jTeacherCourse.getTeacherInheritance().getId(),
        jTeacherCourse.getCourseId(),
        jTeacherCourse.getAssignedAt());
  }
}
