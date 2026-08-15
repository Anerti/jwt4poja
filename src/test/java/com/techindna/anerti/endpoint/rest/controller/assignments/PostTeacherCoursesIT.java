package com.techindna.anerti.endpoint.rest.controller.assignments;

import static org.assertj.core.api.Assertions.assertThat;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.dto.CreateTeacherCourseInput;
import com.techindna.anerti.dto.TeacherCourse;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.CourseRepository;
import com.techindna.anerti.repository.TeacherCourseRepository;
import com.techindna.anerti.repository.TeacherInheritanceRepository;
import com.techindna.anerti.repository.enums.TeacherStatus;
import com.techindna.anerti.repository.enums.UserRole;
import com.techindna.anerti.repository.model.JCourse;
import com.techindna.anerti.repository.model.JTeacherCourse;
import com.techindna.anerti.repository.model.JTeacherInheritance;
import com.techindna.anerti.repository.model.JUser;
import com.techindna.anerti.security.jwt.JwtTokenProvider;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;

@TestConstructor(autowireMode = AutowireMode.ALL)
class PostTeacherCoursesIT extends FacadeIT {

  private final TestRestTemplate restTemplate;
  private final AuthRepository authRepository;
  private final TeacherInheritanceRepository teacherInheritanceRepository;
  private final CourseRepository courseRepository;
  private final TeacherCourseRepository teacherCourseRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;

  PostTeacherCoursesIT(
      TestRestTemplate restTemplate,
      AuthRepository authRepository,
      TeacherInheritanceRepository teacherInheritanceRepository,
      CourseRepository courseRepository,
      TeacherCourseRepository teacherCourseRepository,
      JwtTokenProvider jwtTokenProvider,
      PasswordEncoder passwordEncoder) {
    this.restTemplate = restTemplate;
    this.authRepository = authRepository;
    this.teacherInheritanceRepository = teacherInheritanceRepository;
    this.courseRepository = courseRepository;
    this.teacherCourseRepository = teacherCourseRepository;
    this.jwtTokenProvider = jwtTokenProvider;
    this.passwordEncoder = passwordEncoder;
    restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
  }

  @BeforeEach
  void clean() {
    teacherCourseRepository.deleteAll();
    courseRepository.deleteAll();
    teacherInheritanceRepository.deleteAll();
    authRepository.deleteAll();
  }

  @Test
  void admin_assigns_course_to_teacher() {
    JTeacherInheritance teacher = saveTeacher();
    JCourse course = saveCourse();
    Instant assignedAt = Instant.parse("2023-09-01T08:00:00Z");

    ResponseEntity<TeacherCourse> response =
        postTeacherCourse(validRequest(teacher.getId(), course.getId(), assignedAt), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    TeacherCourse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.teacherId()).isEqualTo(teacher.getId());
    assertThat(body.courseId()).isEqualTo(course.getId());
    assertThat(body.assignedAt()).isEqualTo(assignedAt);

    JTeacherCourse saved = teacherCourseRepository.findAll().getFirst();
    assertThat(saved.getTeacherInheritance().getId()).isEqualTo(teacher.getId());
    assertThat(saved.getCourseId()).isEqualTo(course.getId());
    assertThat(saved.getAssignedAt()).isEqualTo(assignedAt);
  }

  @Test
  void missing_token_is_unauthorized() {
    ResponseEntity<String> response =
        postTeacherCourseError(
            validRequest(
                UUID.randomUUID(), UUID.randomUUID(), Instant.parse("2023-09-01T08:00:00Z")),
            null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("Authentication required.");
  }

  @Test
  void teacher_role_is_forbidden() {
    ResponseEntity<String> response =
        postTeacherCourseError(
            validRequest(
                UUID.randomUUID(), UUID.randomUUID(), Instant.parse("2023-09-01T08:00:00Z")),
            teacherToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Insufficient privileges.");
  }

  @Test
  void student_role_is_forbidden() {
    ResponseEntity<String> response =
        postTeacherCourseError(
            validRequest(
                UUID.randomUUID(), UUID.randomUUID(), Instant.parse("2023-09-01T08:00:00Z")),
            studentToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Insufficient privileges.");
  }

  @Test
  void unknown_teacher_is_not_found() {
    JCourse course = saveCourse();
    UUID unknownTeacher = UUID.randomUUID();

    ResponseEntity<String> response =
        postTeacherCourseError(
            validRequest(unknownTeacher, course.getId(), Instant.parse("2023-09-01T08:00:00Z")),
            adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).contains("Teacher %s not found".formatted(unknownTeacher));
  }

  @Test
  void unknown_course_is_not_found() {
    JTeacherInheritance teacher = saveTeacher();
    UUID unknownCourse = UUID.randomUUID();

    ResponseEntity<String> response =
        postTeacherCourseError(
            validRequest(teacher.getId(), unknownCourse, Instant.parse("2023-09-01T08:00:00Z")),
            adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).contains("Course %s not found".formatted(unknownCourse));
  }

  @Test
  void duplicate_assignment_is_conflict() {
    JTeacherInheritance teacher = saveTeacher();
    JCourse course = saveCourse();
    teacherCourseRepository.save(
        JTeacherCourse.builder()
            .teacherInheritance(teacher)
            .courseId(course.getId())
            .assignedAt(Instant.parse("2023-09-01T08:00:00Z"))
            .build());

    ResponseEntity<String> response =
        postTeacherCourseError(
            validRequest(teacher.getId(), course.getId(), Instant.parse("2023-09-01T08:00:00Z")),
            adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody())
        .contains(
            "Teacher %s is already assigned to course %s"
                .formatted(teacher.getId(), course.getId()));
  }

  @Test
  void null_teacher_id_is_unprocessable() {
    ResponseEntity<String> response =
        postTeacherCourseError(
            validRequest(null, UUID.randomUUID(), Instant.parse("2023-09-01T08:00:00Z")),
            adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("teacherId is required and cannot be blank");
  }

  @Test
  void null_course_id_is_unprocessable() {
    ResponseEntity<String> response =
        postTeacherCourseError(
            validRequest(UUID.randomUUID(), null, Instant.parse("2023-09-01T08:00:00Z")),
            adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("courseId is required and cannot be blank");
  }

  @Test
  void null_assigned_at_is_unprocessable() {
    JTeacherInheritance teacher = saveTeacher();
    JCourse course = saveCourse();

    ResponseEntity<String> response =
        postTeacherCourseError(validRequest(teacher.getId(), course.getId(), null), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("assignedAt is required and cannot be blank");
  }

  @Test
  void malformed_body_is_bad_request() {
    HttpHeaders headers = jsonHeaders(adminToken());

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/teacher-courses",
            HttpMethod.POST,
            new HttpEntity<>("{not-json", headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("Request body is missing or malformed.");
  }

  @Test
  void invalid_uuid_in_body_is_bad_request() {
    JCourse course = saveCourse();
    HttpHeaders headers = jsonHeaders(adminToken());
    String body =
        "{\"teacherId\":\"not-a-uuid\",\"courseId\":\"%s\",\"assignedAt\":\"2023-09-01T08:00:00Z\"}"
            .formatted(course.getId());

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/teacher-courses", HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("Request body is missing or malformed.");
  }

  @Test
  void invalid_assigned_at_is_bad_request() {
    JTeacherInheritance teacher = saveTeacher();
    JCourse course = saveCourse();
    HttpHeaders headers = jsonHeaders(adminToken());
    String body =
        "{\"teacherId\":\"%s\",\"courseId\":\"%s\",\"assignedAt\":\"not-a-date\"}"
            .formatted(teacher.getId(), course.getId());

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/teacher-courses", HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("Request body is missing or malformed.");
  }

  private JTeacherInheritance saveTeacher() {
    return teacherInheritanceRepository.save(
        JTeacherInheritance.builder().ref("T001").teacherStatus(TeacherStatus.ACTIVE).build());
  }

  private JCourse saveCourse() {
    return courseRepository.save(
        JCourse.builder().ref("EP1").title("Digital Electronics").credits(6).build());
  }

  private CreateTeacherCourseInput validRequest(UUID teacherId, UUID courseId, Instant assignedAt) {
    return new CreateTeacherCourseInput(teacherId, courseId, assignedAt);
  }

  private JUser saveUser(String username, String email, UserRole role) {
    return authRepository.save(
        JUser.builder()
            .username(username)
            .password(passwordEncoder.encode("StrongPass12!"))
            .firstName("Some")
            .lastName("User")
            .email(email)
            .role(role)
            .build());
  }

  private String adminToken() {
    JUser admin = saveUser("root_admin", "root.admin@hacheuil.edu", UserRole.ADMIN);
    return jwtTokenProvider.generateToken(admin.getId().toString(), admin.getRole().name());
  }

  private String teacherToken() {
    JUser teacher = saveUser("terry_teacher", "terry.teacher@hacheuil.edu", UserRole.TEACHER);
    return jwtTokenProvider.generateToken(teacher.getId().toString(), teacher.getRole().name());
  }

  private String studentToken() {
    JUser student = saveUser("sam_student", "sam.student@hacheuil.edu", UserRole.STUDENT);
    return jwtTokenProvider.generateToken(student.getId().toString(), student.getRole().name());
  }

  private ResponseEntity<TeacherCourse> postTeacherCourse(
      CreateTeacherCourseInput request, String token) {
    return restTemplate.exchange(
        "/teacher-courses",
        HttpMethod.POST,
        new HttpEntity<>(request, jsonHeaders(token)),
        TeacherCourse.class);
  }

  private ResponseEntity<String> postTeacherCourseError(
      CreateTeacherCourseInput request, String token) {
    return restTemplate.exchange(
        "/teacher-courses",
        HttpMethod.POST,
        new HttpEntity<>(request, jsonHeaders(token)),
        String.class);
  }

  private HttpHeaders jsonHeaders(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (token != null) {
      headers.setBearerAuth(token);
    }
    return headers;
  }
}
