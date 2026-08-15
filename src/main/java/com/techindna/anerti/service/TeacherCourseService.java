package com.techindna.anerti.service;

import com.techindna.anerti.dto.CreateTeacherCourseInput;
import com.techindna.anerti.dto.TeacherCourse;
import com.techindna.anerti.exception.http.ConflictException;
import com.techindna.anerti.exception.http.NotFoundException;
import com.techindna.anerti.mapper.TeacherCourseMapper;
import com.techindna.anerti.repository.TeacherCourseRepository;
import com.techindna.anerti.repository.model.JTeacherCourse;
import com.techindna.anerti.validator.TeacherCourseValidator;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class TeacherCourseService {

  private final TeacherCourseRepository teacherCourseRepository;
  private final TeacherCourseValidator teacherCourseValidator;
  private final TeacherCourseMapper teacherCourseMapper;

  @Transactional
  public TeacherCourse createTeacherCourse(CreateTeacherCourseInput request) {
    teacherCourseValidator.validateCreateTeacherCourse(request);

    try {
      JTeacherCourse saved =
          teacherCourseRepository.saveAndFlush(teacherCourseMapper.toRepository(request));
      return teacherCourseMapper.toDto(saved);
    } catch (DataIntegrityViolationException e) {
      String message = e.getMostSpecificCause().getMessage();
      if (message.contains("teacher_course_teacher_inheritance_id_fkey")) {
        throw new NotFoundException("Teacher %s not found".formatted(request.teacherId()));
      }
      if (message.contains("teacher_course_course_id_fkey")) {
        throw new NotFoundException("Course %s not found".formatted(request.courseId()));
      }
      if (message.contains("teacher_course_teacher_inheritance_id_course_id_key")) {
        throw new ConflictException(
            "Teacher %s is already assigned to course %s"
                .formatted(request.teacherId(), request.courseId()));
      }
      throw e;
    }
  }
}
