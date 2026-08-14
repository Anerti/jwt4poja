package com.techindna.anerti.service;

import com.techindna.anerti.dto.CourseListResponse;
import com.techindna.anerti.dto.CourseOutput;
import com.techindna.anerti.dto.CreateCourseInput;
import com.techindna.anerti.dto.Meta;
import com.techindna.anerti.exception.http.ConflictException;
import com.techindna.anerti.mapper.CourseMapper;
import com.techindna.anerti.repository.CourseRepository;
import com.techindna.anerti.repository.enums.CourseType;
import com.techindna.anerti.repository.model.JCourse;
import com.techindna.anerti.validator.CourseValidator;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class CourseService {

  private final CourseRepository courseRepository;
  private final CourseValidator courseValidator;
  private final CourseMapper courseMapper;

  @Transactional(readOnly = true)
  public CourseListResponse listCourses(String search, CourseType type, int page, int size) {
    courseValidator.validateListFilters(search);

    int validPage = defaultIfInvalid(page, 1, 100, 1);
    int validSize = defaultIfInvalid(size, 1, 100, 10);
    Pageable pageable = PageRequest.of(validPage - 1, validSize, Sort.by("createdAt", "ref"));

    Page<JCourse> jCourses =
        courseRepository.search(search, type == null ? null : type.name(), pageable);

    List<CourseOutput> courses =
        jCourses.getContent().stream()
            .map(courseMapper::toEntity)
            .map(courseMapper::toDto)
            .toList();
    return new CourseListResponse(
        courses, new Meta(validPage, validSize, jCourses.getTotalElements()));
  }

  private int defaultIfInvalid(int value, int min, int max, int defaultValue) {
    return (value < min || value > max) ? defaultValue : value;
  }

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
