package com.techindna.anerti.repository;

import com.techindna.anerti.repository.model.JCourse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<JCourse, UUID> {

  Optional<JCourse> findByRef(String ref);
}
