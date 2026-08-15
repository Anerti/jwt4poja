package com.techindna.anerti.repository;

import com.techindna.anerti.repository.model.JClass;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassRepository extends JpaRepository<JClass, UUID> {

  Optional<JClass> findByName(String name);
}
