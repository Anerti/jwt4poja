package com.techindna.anerti.endpoint.rest.controller.exams;

import static org.assertj.core.api.Assertions.assertThat;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.dto.ExamListResponse;
import com.techindna.anerti.dto.ExamOutput;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.CourseRepository;
import com.techindna.anerti.repository.ExamRepository;
import com.techindna.anerti.repository.TeacherCourseRepository;
import com.techindna.anerti.repository.TeacherInheritanceRepository;
import com.techindna.anerti.repository.enums.TeacherStatus;
import com.techindna.anerti.repository.enums.UserRole;
import com.techindna.anerti.repository.model.JCourse;
import com.techindna.anerti.repository.model.JExam;
import com.techindna.anerti.repository.model.JTeacherCourse;
import com.techindna.anerti.repository.model.JTeacherInheritance;
import com.techindna.anerti.repository.model.JUser;
import com.techindna.anerti.security.jwt.JwtTokenProvider;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;
import org.springframework.web.util.UriComponentsBuilder;

@TestConstructor(autowireMode = AutowireMode.ALL)
class GetExamsIT extends FacadeIT {

  private final TestRestTemplate restTemplate;
  private final AuthRepository authRepository;
  private final TeacherInheritanceRepository teacherInheritanceRepository;
  private final CourseRepository courseRepository;
  private final TeacherCourseRepository teacherCourseRepository;
  private final ExamRepository examRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;

  GetExamsIT(
      TestRestTemplate restTemplate,
      AuthRepository authRepository,
      TeacherInheritanceRepository teacherInheritanceRepository,
      CourseRepository courseRepository,
      TeacherCourseRepository teacherCourseRepository,
      ExamRepository examRepository,
      JwtTokenProvider jwtTokenProvider,
      PasswordEncoder passwordEncoder) {
    this.restTemplate = restTemplate;
    this.authRepository = authRepository;
    this.teacherInheritanceRepository = teacherInheritanceRepository;
    this.courseRepository = courseRepository;
    this.teacherCourseRepository = teacherCourseRepository;
    this.examRepository = examRepository;
    this.jwtTokenProvider = jwtTokenProvider;
    this.passwordEncoder = passwordEncoder;
    restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
  }

  @BeforeEach
  void clean() {
    examRepository.deleteAll();
    teacherCourseRepository.deleteAll();
    authRepository.deleteAll();
    courseRepository.deleteAll();
    teacherInheritanceRepository.deleteAll();
  }

  @Test
  void admin_sees_all_exams() {
    JCourse c1 = saveCourse("EP1");
    JCourse c2 = saveCourse("EP2");
    saveExam(c1, "0.5", 2023, "2024-01-15T09:00:00Z");
    saveExam(c2, "0.5", 2024, "2025-01-15T09:00:00Z");

    ResponseEntity<ExamListResponse> response = getExams(adminToken(), null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().data()).hasSize(2);
    assertThat(response.getBody().meta().total()).isEqualTo(2);
  }

  @Test
  void teacher_only_sees_exams_of_assigned_courses() {
    JTeacherInheritance teacher = saveTeacher();
    JCourse c1 = saveCourse("EP1");
    JCourse c2 = saveCourse("EP2");
    assign(teacher, c1);
    saveExam(c1, "0.5", 2023, "2024-01-15T09:00:00Z");
    saveExam(c2, "0.5", 2023, "2024-02-15T09:00:00Z");

    ResponseEntity<ExamListResponse> response = getExams(teacherToken(teacher), null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().data()).extracting(ExamOutput::courseId).containsOnly(c1.getId());
  }

  @Test
  void filters_exams_by_course_ref() {
    JCourse c1 = saveCourse("EP1");
    JCourse c2 = saveCourse("EP2");
    saveExam(c1, "0.5", 2023, "2024-01-15T09:00:00Z");
    saveExam(c2, "0.5", 2023, "2024-02-15T09:00:00Z");

    ResponseEntity<ExamListResponse> response =
        getExams(adminToken(), UriComponentsBuilder.fromPath("/exams").queryParam("ref", "EP1"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().data()).hasSize(1);
    assertThat(response.getBody().data().getFirst().courseId()).isEqualTo(c1.getId());
  }

  @Test
  void filters_exams_by_academic_year() {
    JCourse c1 = saveCourse("EP1");
    saveExam(c1, "0.5", 2023, "2024-01-15T09:00:00Z");
    saveExam(c1, "0.5", 2024, "2025-01-15T09:00:00Z");

    ResponseEntity<ExamListResponse> response =
        getExams(
            adminToken(),
            UriComponentsBuilder.fromPath("/exams").queryParam("academicYear", "2023"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().data()).hasSize(1);
    assertThat(response.getBody().data().getFirst().academicYear()).isEqualTo(2023);
  }

  @Test
  void filters_exams_by_date_range() {
    JCourse c1 = saveCourse("EP1");
    saveExam(c1, "0.5", 2023, "2024-01-15T09:00:00Z");
    saveExam(c1, "0.5", 2023, "2024-02-15T09:00:00Z");

    ResponseEntity<ExamListResponse> response =
        getExams(
            adminToken(),
            UriComponentsBuilder.fromPath("/exams")
                .queryParam("startDate", "2024-02-01T00:00:00Z")
                .queryParam("endDate", "2024-03-01T00:00:00Z"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().data()).hasSize(1);
    assertThat(response.getBody().data().getFirst().date())
        .isEqualTo(Instant.parse("2024-02-15T09:00:00Z"));
  }

  @Test
  void start_date_after_end_date_is_unprocessable() {
    ResponseEntity<String> response =
        getExamsError(
            adminToken(),
            UriComponentsBuilder.fromPath("/exams")
                .queryParam("startDate", "2024-03-01T00:00:00Z")
                .queryParam("endDate", "2024-02-01T00:00:00Z"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("startDate must be before or equal to endDate");
  }

  @Test
  void exams_are_paginated() {
    JCourse c1 = saveCourse("EP1");
    saveExam(c1, "0.5", 2023, "2024-01-15T09:00:00Z");
    saveExam(c1, "0.5", 2023, "2024-02-15T09:00:00Z");
    saveExam(c1, "0.5", 2023, "2024-03-15T09:00:00Z");

    ResponseEntity<ExamListResponse> response =
        getExams(
            adminToken(),
            UriComponentsBuilder.fromPath("/exams")
                .queryParam("page", "1")
                .queryParam("size", "2"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().data()).hasSize(2);
    assertThat(response.getBody().meta().total()).isEqualTo(3);
    assertThat(response.getBody().data().getFirst().date())
        .isEqualTo(Instant.parse("2024-03-15T09:00:00Z"));
  }

  @Test
  void student_role_is_forbidden() {
    ResponseEntity<String> response = getExamsError(studentToken(), null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Insufficient privileges.");
  }

  private void saveExam(JCourse course, String coefficient, int academicYear, String date) {
    examRepository.save(
        JExam.builder()
            .course(course)
            .coefficient(new BigDecimal(coefficient))
            .academicYear(academicYear)
            .date(Instant.parse(date))
            .build());
  }

  private JTeacherInheritance saveTeacher() {
    return teacherInheritanceRepository.save(
        JTeacherInheritance.builder().ref("T001").teacherStatus(TeacherStatus.ACTIVE).build());
  }

  private JCourse saveCourse(String ref) {
    return courseRepository.save(
        JCourse.builder().ref(ref).title("Digital Electronics").credits(6).build());
  }

  private void assign(JTeacherInheritance teacher, JCourse course) {
    teacherCourseRepository.save(
        JTeacherCourse.builder()
            .teacherInheritance(teacher)
            .courseId(course.getId())
            .assignedAt(Instant.parse("2023-09-01T08:00:00Z"))
            .build());
  }

  private JUser saveUser(
      String username, String email, UserRole role, JTeacherInheritance teacherInheritance) {
    return authRepository.save(
        JUser.builder()
            .username(username)
            .password(passwordEncoder.encode("StrongPass12!"))
            .firstName("Some")
            .lastName("User")
            .email(email)
            .role(role)
            .teacherInheritance(teacherInheritance)
            .build());
  }

  private String adminToken() {
    JUser admin = saveUser("root_admin", "root.admin@hacheuil.edu", UserRole.ADMIN, null);
    return jwtTokenProvider.generateToken(admin.getId().toString(), admin.getRole().name());
  }

  private String teacherToken(JTeacherInheritance teacher) {
    JUser teacherUser =
        saveUser("terry_teacher", "terry.teacher@hacheuil.edu", UserRole.TEACHER, teacher);
    return jwtTokenProvider.generateToken(
        teacherUser.getId().toString(), teacherUser.getRole().name());
  }

  private String studentToken() {
    JUser student = saveUser("sam_student", "sam.student@hacheuil.edu", UserRole.STUDENT, null);
    return jwtTokenProvider.generateToken(student.getId().toString(), student.getRole().name());
  }

  private ResponseEntity<ExamListResponse> getExams(String token, UriComponentsBuilder builder) {
    String url = builder == null ? "/exams" : builder.build().encode().toUriString();
    return restTemplate.exchange(
        url, HttpMethod.GET, new HttpEntity<>(jsonHeaders(token)), ExamListResponse.class);
  }

  private ResponseEntity<String> getExamsError(String token, UriComponentsBuilder builder) {
    String url = builder == null ? "/exams" : builder.build().encode().toUriString();
    return restTemplate.exchange(
        url, HttpMethod.GET, new HttpEntity<>(jsonHeaders(token)), String.class);
  }

  private HttpHeaders jsonHeaders(String token) {
    HttpHeaders headers = new HttpHeaders();
    if (token != null) {
      headers.setBearerAuth(token);
    }
    return headers;
  }
}
