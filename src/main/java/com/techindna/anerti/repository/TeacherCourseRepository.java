package com.techindna.anerti.repository;

import com.techindna.anerti.repository.model.JTeacherCourse;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherCourseRepository extends JpaRepository<JTeacherCourse, UUID> {}
