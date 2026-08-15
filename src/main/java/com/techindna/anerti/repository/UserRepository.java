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

  @Query(
      """
      SELECT u FROM JUser u
      JOIN u.studentInheritance si
      WHERE u.role = :role
        AND (:search IS NULL OR :search = ''
          OR LOWER(si.ref) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
          OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
          OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
          AND (CAST(:learningPath AS string) IS NULL
            OR CAST(si.learningPath AS string) = :learningPath)
          AND (CAST(:studentStatus AS string) IS NULL
            OR CAST(si.studentStatus AS string) = :studentStatus)
          AND (:className IS NULL OR :className = ''
            OR EXISTS (
              SELECT 1 FROM JClass c WHERE c.id = si.classId AND c.name = :className))
      """)
  Page<JUser> searchStudents(
      @Param("search") String search,
      @Param("learningPath") String learningPath,
      @Param("studentStatus") String studentStatus,
      @Param("className") String className,
      @Param("role") UserRole role,
      Pageable pageable);
}
