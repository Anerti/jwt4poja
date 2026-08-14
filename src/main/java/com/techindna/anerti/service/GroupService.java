package com.techindna.anerti.service;

import com.techindna.anerti.dto.CreateGroupInput;
import com.techindna.anerti.dto.GroupOutput;
import com.techindna.anerti.exception.http.ConflictException;
import com.techindna.anerti.mapper.GroupMapper;
import com.techindna.anerti.repository.GroupRepository;
import com.techindna.anerti.repository.model.JGroup;
import com.techindna.anerti.validator.DataValidator;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class GroupService {

  private final GroupRepository groupRepository;
  private final DataValidator dataValidator;
  private final GroupMapper groupMapper;

  @Transactional
  public GroupOutput createGroup(CreateGroupInput request) {
    dataValidator.validateRef(request.ref());

    try {
      JGroup saved = groupRepository.saveAndFlush(groupMapper.toRepository(request));
      return groupMapper.toDto(groupMapper.toEntity(saved));
    } catch (DataIntegrityViolationException e) {
      if (e.getMostSpecificCause().getMessage().contains("ref")) {
        throw new ConflictException("Cannot use ref %s".formatted(request.ref().strip()));
      }
      throw e;
    }
  }
}
