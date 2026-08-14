package com.techindna.anerti.service;

import com.techindna.anerti.dto.CourseOutput;
import com.techindna.anerti.dto.CreateCourseInput;
import com.techindna.anerti.exception.http.ConflictException;
import com.techindna.anerti.mapper.CourseMapper;
import com.techindna.anerti.repository.CourseRepository;
import com.techindna.anerti.repository.model.JCourse;
import com.techindna.anerti.validator.CourseValidator;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class CourseService {

  private final CourseRepository courseRepository;
  private final CourseValidator courseValidator;
  private final CourseMapper courseMapper;

  @Transactional
  public CourseOutput createCourse(CreateCourseInput request) {
    courseValidator.validateCreateCourse(request);

    try {
      JCourse saved = courseRepository.saveAndFlush(courseMapper.toRepository(request));
      return courseMapper.toDto(courseMapper.toEntity(saved));
    } catch (DataIntegrityViolationException e) {
      if (e.getMostSpecificCause().getMessage().contains("ref")) {
        throw new ConflictException("Cannot use ref %s".formatted(request.ref().strip()));
      }
      throw e;
    }
  }
}
