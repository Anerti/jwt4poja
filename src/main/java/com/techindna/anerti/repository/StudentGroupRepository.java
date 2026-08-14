package com.techindna.anerti.repository;

import com.techindna.anerti.repository.model.JStudentGroup;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentGroupRepository extends JpaRepository<JStudentGroup, UUID> {}
