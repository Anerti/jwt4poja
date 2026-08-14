package com.techindna.anerti.repository;

import com.techindna.anerti.repository.model.JGroup;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<JGroup, UUID> {

  Optional<JGroup> findByRef(String ref);
}
