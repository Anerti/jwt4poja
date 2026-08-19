package com.techindna.anerti.mapper;

import com.techindna.anerti.dto.ClassOutput;
import com.techindna.anerti.dto.CreateClassInput;
import com.techindna.anerti.repository.model.JClass;
import org.springframework.stereotype.Component;

@Component
public class ClassMapper {

  public JClass toRepository(CreateClassInput request) {
    return JClass.builder().name(request.name().strip()).yearOf(request.yearOf()).build();
  }

  public ClassOutput toDto(JClass jClass) {
    return new ClassOutput(
        jClass.getId(),
        jClass.getName(),
        jClass.getYearOf(),
        jClass.getCreatedAt(),
        jClass.getUpdatedAt());
  }
}
