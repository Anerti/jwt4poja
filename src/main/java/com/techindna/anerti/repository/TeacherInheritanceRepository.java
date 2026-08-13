package com.techindna.anerti.repository;

import com.techindna.anerti.repository.model.JTeacherInheritance;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherInheritanceRepository extends JpaRepository<JTeacherInheritance, UUID> {

  Optional<JTeacherInheritance> findByRef(String ref);
}
