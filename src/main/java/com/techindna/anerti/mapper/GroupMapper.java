package com.techindna.anerti.mapper;

import com.techindna.anerti.dto.CreateGroupInput;
import com.techindna.anerti.dto.GroupOutput;
import com.techindna.anerti.entity.Group;
import com.techindna.anerti.repository.enums.CourseType;
import com.techindna.anerti.repository.model.JGroup;
import org.springframework.stereotype.Component;

@Component
public class GroupMapper {

  public JGroup toRepository(CreateGroupInput request) {
    return JGroup.builder()
        .ref(request.ref().strip())
        .type(request.type() != null ? request.type() : CourseType.COMMON)
        .build();
  }

  public Group toEntity(JGroup jGroup) {
    return new Group(jGroup.getId(), jGroup.getRef(), jGroup.getType(), jGroup.getCreatedAt());
  }

  public GroupOutput toDto(Group group) {
    return new GroupOutput(group.id(), group.ref(), group.type(), group.createdAt());
  }
}
