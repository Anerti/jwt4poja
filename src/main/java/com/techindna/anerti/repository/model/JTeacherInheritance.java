package com.techindna.anerti.repository.model;

import com.techindna.anerti.repository.enums.TeacherStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "teacher_inheritance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JTeacherInheritance {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 10)
  private String ref;

  @Column(name = "joigned_at")
  private Instant joinedAt;

  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "teacher_status", nullable = false)
  @Builder.Default
  private TeacherStatus teacherStatus = TeacherStatus.ACTIVE;

  @OneToMany(mappedBy = "teacherInheritance")
  @Builder.Default
  private List<JTeacherCourse> teacherCourses = new ArrayList<>();
}
