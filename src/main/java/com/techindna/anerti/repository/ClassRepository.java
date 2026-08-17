package com.techindna.anerti.repository;

import com.techindna.anerti.repository.model.JClass;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassRepository extends JpaRepository<JClass, UUID> {

  Optional<JClass> findByName(String name);

  @Query(
      value =
          """
          SELECT c.id, c.name, c.year_of, c.created_at, c.updated_at
          FROM jwt4poja_app."class" c
          WHERE (CAST(:search AS text) IS NULL OR :search = ''
            OR LOWER(c.name) LIKE LOWER('%' || :search || '%'))
          ORDER BY c.year_of DESC, c.name ASC
          """,
      countQuery =
          """
          SELECT COUNT(*) FROM jwt4poja_app."class" c
          WHERE (CAST(:search AS text) IS NULL OR :search = ''
            OR LOWER(c.name) LIKE LOWER('%' || :search || '%'))
          """,
      nativeQuery = true)
  Page<JClass> search(@Param("search") String search, Pageable pageable);
}
