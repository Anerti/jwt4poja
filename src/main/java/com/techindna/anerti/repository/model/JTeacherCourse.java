package com.techindna.anerti.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "teacher_course")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JTeacherCourse {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "teacher_inheritance_id", nullable = false)
  private JTeacherInheritance teacherInheritance;

  @Column(name = "course_id", nullable = false)
  private UUID courseId;

  @Column(name = "assigned_at", nullable = false)
  private Instant assignedAt;
}
