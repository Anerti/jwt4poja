package com.techindna.anerti.repository;

import com.techindna.anerti.repository.enums.UserRole;
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
      JOIN u.teacherInheritance ti
      WHERE u.role = :role
        AND (:search IS NULL OR :search = ''
          OR LOWER(ti.ref) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
          OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
          OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            AND (CAST(:teacherStatus AS string) IS NULL
              OR CAST(ti.teacherStatus AS string) = :teacherStatus)
      """)
  Page<JUser> searchTeachers(
      @Param("search") String search,
      @Param("teacherStatus") String teacherStatus,
      @Param("role") UserRole role,
      Pageable pageable);
}
