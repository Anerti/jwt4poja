package com.techindna.anerti.security;

import com.techindna.anerti.entity.enums.UserRole;
import com.techindna.anerti.exception.http.ForbiddenException;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ResourcesAccessRules {

  public void grantAccessFor(UUID targetId, UserRole targetRole) {
    UserRole requesterRole = requesterRole("Insufficient privileges to access this resource");
    UUID requesterId =
        UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());

    if (requesterId.equals(targetId)) {
      return;
    }

    if ((requesterRole == UserRole.CUSTOMER)
        || (requesterRole == UserRole.ADMIN && targetRole == UserRole.ADMIN)) {
      throw new ForbiddenException("Insufficient privileges to access this resource");
    }
  }

  public void grantOwnerOnlyAccess(UUID targetId, String forbiddenMessage) {
    UUID requesterId =
        UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
    if (!requesterId.equals(targetId)) {
      throw new ForbiddenException(forbiddenMessage);
    }
  }

  private UserRole requesterRole(String forbiddenMessage) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    var authorities = auth.getAuthorities();
    if (authorities.isEmpty()) {
      throw new ForbiddenException(forbiddenMessage);
    }
    return UserRole.valueOf(
        Objects.requireNonNull(authorities.iterator().next().getAuthority()).replace("ROLE_", ""));
  }
}
