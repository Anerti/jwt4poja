package com.techindna.anerti.security;

import com.techindna.anerti.exception.http.ForbiddenException;
import com.techindna.anerti.repository.enums.UserRole;
import com.techindna.anerti.repository.model.JUser;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AccessRules {

  public void requireAssignedToCourse(JUser user, UUID courseId, boolean assigned) {
    if (!assigned && (user.getRole() != UserRole.ADMIN)) {
      throw new ForbiddenException("Cannot create exam for course %s".formatted(courseId));
    }
  }
}
