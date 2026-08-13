package com.techindna.anerti.repository.model;

import com.techindna.anerti.repository.enums.LearningPath;
import com.techindna.anerti.repository.enums.Level;
import com.techindna.anerti.repository.enums.StudentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "student_inheritance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JStudentInheritance {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 10)
  private String ref;

  @Column(name = "joigned_at")
  private Instant joinedAt;

  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false)
  private Level level;

  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "learning_path", nullable = false)
  @Builder.Default
  private LearningPath learningPath = LearningPath.COMMON;

  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "student_status", nullable = false)
  @Builder.Default
  private StudentStatus studentStatus = StudentStatus.ACTIVE;

  @Column(name = "graduation_year")
  private Integer graduationYear;

  @Column(name = "class_name", length = 30)
  private String className;
}
