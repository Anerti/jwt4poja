package com.techindna.anerti.service;

import com.techindna.anerti.dto.ClassListResponse;
import com.techindna.anerti.dto.ClassOutput;
import com.techindna.anerti.dto.CreateClassInput;
import com.techindna.anerti.dto.Meta;
import com.techindna.anerti.exception.http.ConflictException;
import com.techindna.anerti.exception.http.UnauthorizedException;
import com.techindna.anerti.mapper.ClassMapper;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.ClassRepository;
import com.techindna.anerti.repository.model.JClass;
import com.techindna.anerti.repository.model.JUser;
import com.techindna.anerti.validator.ClassValidator;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ClassService {

  private final ClassRepository classRepository;
  private final AuthRepository authRepository;
  private final ClassValidator classValidator;
  private final ClassMapper classMapper;

  @Transactional(readOnly = true)
  public ClassListResponse listClasses(String search, int page, int size) {
    classValidator.validateListFilters(search);
    currentUser();

    PageRequestData p = PageRequestData.of(page, size, Sort.unsorted());
    Page<JClass> jClasses = classRepository.search(search, p.pageable());

    List<ClassOutput> classes = jClasses.getContent().stream().map(classMapper::toDto).toList();
    return new ClassListResponse(
        classes, new Meta(p.page(), p.size(), jClasses.getTotalElements()));
  }

  @Transactional
  public ClassOutput createClass(CreateClassInput request) {
    classValidator.validateCreateClass(request);
    currentUser();

    String name = request.name().strip();
    classRepository
        .findByName(name)
        .ifPresent(
            c -> {
              throw new ConflictException("Class %s already exists".formatted(name));
            });

    JClass saved = classRepository.saveAndFlush(classMapper.toRepository(request));
    return classMapper.toDto(saved);
  }

  private JUser currentUser() {
    return authRepository
        .findById(
            UUID.fromString(
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal()))
        .orElseThrow(() -> new UnauthorizedException("Authentication required."));
  }
}
