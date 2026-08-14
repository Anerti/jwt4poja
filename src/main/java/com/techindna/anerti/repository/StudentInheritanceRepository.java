package com.techindna.anerti.repository;

import com.techindna.anerti.repository.model.JStudentInheritance;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentInheritanceRepository extends JpaRepository<JStudentInheritance, UUID> {

  Optional<JStudentInheritance> findByRef(String ref);
}
