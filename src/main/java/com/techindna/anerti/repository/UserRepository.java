package com.techindna.anerti.repository;

import com.techindna.anerti.entity.enums.UserRole;
import com.techindna.anerti.repository.model.JUser;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<JUser, UUID> {

  @Query(
      """
      SELECT u FROM JUser u
      WHERE u.role = :role
        AND (:search IS NULL OR :search = ''
          OR lower(u.username) LIKE lower(concat(concat('%', :search), '%'))
          OR lower(u.firstName) LIKE lower(concat(concat('%', :search), '%'))
          OR lower(u.lastName) LIKE lower(concat(concat('%', :search), '%'))
          OR lower(u.email) LIKE lower(concat(concat('%', :search), '%')))
      """)
  Page<JUser> searchUsers(
      @Param("role") UserRole role, @Param("search") String search, Pageable pageable);
}
