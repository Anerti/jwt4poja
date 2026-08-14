package com.techindna.anerti.repository;

import com.techindna.anerti.repository.model.JGroup;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<JGroup, UUID> {

  Optional<JGroup> findByRef(String ref);

  @Query(
      """
      SELECT g FROM JGroup g
      WHERE (:search IS NULL OR :search = ''
        OR LOWER(g.ref) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
        AND (CAST(:type AS string) IS NULL OR CAST(g.type AS string) = :type)
      """)
  Page<JGroup> search(
      @Param("search") String search, @Param("type") String type, Pageable pageable);
}
