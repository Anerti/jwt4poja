package com.techindna.anerti.repository;

import com.techindna.anerti.repository.model.JGradeHistory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GradeHistoryRepository extends JpaRepository<JGradeHistory, UUID> {

  List<JGradeHistory> findByGradeIdOrderByCreatedAtAsc(UUID gradeId);

  Optional<JGradeHistory> findTopByGradeIdOrderByCreatedAtDesc(UUID gradeId);
}
