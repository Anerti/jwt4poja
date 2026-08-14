package com.techindna.anerti.repository;

import com.techindna.anerti.repository.model.JCourse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<JCourse, UUID> {

  Optional<JCourse> findByRef(String ref);

  @Query(
      """
      SELECT c FROM JCourse c
      WHERE (:search IS NULL OR :search = ''
        OR LOWER(c.ref) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
        OR LOWER(c.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
        AND (CAST(:type AS string) IS NULL OR CAST(c.type AS string) = :type)
      """)
  Page<JCourse> search(
      @Param("search") String search, @Param("type") String type, Pageable pageable);
}
