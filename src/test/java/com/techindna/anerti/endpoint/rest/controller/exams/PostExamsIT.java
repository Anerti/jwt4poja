package com.techindna.anerti.endpoint.rest.controller.exams;

import static org.assertj.core.api.Assertions.assertThat;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.dto.CreateExamInput;
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
class PostExamsIT extends FacadeIT {

  private final TestRestTemplate restTemplate;
  private final AuthRepository authRepository;
  private final CourseRepository courseRepository;
  private final ExamRepository examRepository;
  private final TeacherInheritanceRepository teacherInheritanceRepository;
  private final TeacherCourseRepository teacherCourseRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;

  PostExamsIT(
      TestRestTemplate restTemplate,
      AuthRepository authRepository,
      CourseRepository courseRepository,
      ExamRepository examRepository,
      TeacherInheritanceRepository teacherInheritanceRepository,
      TeacherCourseRepository teacherCourseRepository,
      JwtTokenProvider jwtTokenProvider,
      PasswordEncoder passwordEncoder) {
    this.restTemplate = restTemplate;
    this.authRepository = authRepository;
    this.courseRepository = courseRepository;
    this.examRepository = examRepository;
    this.teacherInheritanceRepository = teacherInheritanceRepository;
    this.teacherCourseRepository = teacherCourseRepository;
    this.jwtTokenProvider = jwtTokenProvider;
    this.passwordEncoder = passwordEncoder;
    restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
  }

  @BeforeEach
  void clean() {
    examRepository.deleteAll();
    teacherCourseRepository.deleteAll();
    courseRepository.deleteAll();
    authRepository.deleteAll();
    teacherInheritanceRepository.deleteAll();
  }

  @Test
  void admin_creates_exam() {
    JCourse course = saveCourse();

    ResponseEntity<ExamOutput> response =
        postExam(
            validRequest(course.getId(), "0.5", "2024-2025", "2024-01-15T09:00:00Z"), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    ExamOutput body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.courseId()).isEqualTo(course.getId());
    assertThat(body.coefficient()).isEqualByComparingTo("0.5");
    assertThat(body.academicYear()).isEqualTo("2024-2025");
    assertThat(body.date()).isEqualTo(Instant.parse("2024-01-15T09:00:00Z"));
    assertThat(body.createdAt()).isNotNull();

    JExam saved = examRepository.findAll().getFirst();
    assertThat(saved.getCourseId()).isEqualTo(course.getId());
    assertThat(saved.getCoefficient()).isEqualByComparingTo("0.5");
    assertThat(saved.getAcademicYear()).isEqualTo("2024-2025");
    assertThat(saved.getDate()).isEqualTo(Instant.parse("2024-01-15T09:00:00Z"));
  }

  @Test
  void teacher_creates_exam() {
    JCourse course = saveCourse();

    ResponseEntity<ExamOutput> response =
        postExam(
            validRequest(course.getId(), "0.5", "2024-2025", "2024-01-15T09:00:00Z"),
            teacherTokenWithCourse(course));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().courseId()).isEqualTo(course.getId());
  }

  @Test
  void teacher_without_assignment_is_forbidden() {
    JCourse course = saveCourse();

    ResponseEntity<String> response =
        postExamError(
            validRequest(course.getId(), "0.5", "2024-2025", "2024-01-15T09:00:00Z"),
            teacherTokenWithoutCourse());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Cannot create exam for course");
  }

  @Test
  void teacher_assigned_to_another_course_is_forbidden() {
    JCourse course = saveCourse();
    JCourse otherCourse =
        courseRepository.save(JCourse.builder().ref("PRJ1").title("Project").credits(6).build());

    ResponseEntity<String> response =
        postExamError(
            validRequest(course.getId(), "0.5", "2024-2025", "2024-01-15T09:00:00Z"),
            teacherTokenWithCourse(otherCourse));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Cannot create exam for course");
  }

  @Test
  void missing_token_is_unauthorized() {
    ResponseEntity<String> response =
        postExamError(
            validRequest(UUID.randomUUID(), "0.5", "2024-2025", "2024-01-15T09:00:00Z"), null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("Authentication required.");
  }

  @Test
  void student_role_is_forbidden() {
    ResponseEntity<String> response =
        postExamError(
            validRequest(UUID.randomUUID(), "0.5", "2024-2025", "2024-01-15T09:00:00Z"),
            studentToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Insufficient privileges.");
  }

  @Test
  void unknown_course_is_not_found() {
    UUID unknownCourse = UUID.randomUUID();

    ResponseEntity<String> response =
        postExamError(
            validRequest(unknownCourse, "0.5", "2024-2025", "2024-01-15T09:00:00Z"), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).contains("Course %s not found".formatted(unknownCourse));
  }

  @Test
  void coefficients_sum_over_one_is_conflict() {
    JCourse course = saveCourse();
    examRepository.save(
        JExam.builder()
            .courseId(course.getId())
            .coefficient(new BigDecimal("0.7"))
            .academicYear("2024-2025")
            .date(Instant.parse("2024-01-10T09:00:00Z"))
            .build());

    ResponseEntity<String> response =
        postExamError(
            validRequest(course.getId(), "0.5", "2024-2025", "2024-01-15T09:00:00Z"), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).contains("already sum to 0.7");
  }

  @Test
  void coefficients_sum_exactly_one_is_accepted() {
    JCourse course = saveCourse();
    examRepository.save(
        JExam.builder()
            .courseId(course.getId())
            .coefficient(new BigDecimal("0.7"))
            .academicYear("2024-2025")
            .date(Instant.parse("2024-01-10T09:00:00Z"))
            .build());

    ResponseEntity<ExamOutput> response =
        postExam(
            validRequest(course.getId(), "0.3", "2024-2025", "2024-01-15T09:00:00Z"), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  }

  @Test
  void same_course_other_academic_year_is_not_conflict() {
    JCourse course = saveCourse();
    examRepository.save(
        JExam.builder()
            .courseId(course.getId())
            .coefficient(new BigDecimal("0.9"))
            .academicYear("2023-2024")
            .date(Instant.parse("2023-01-10T09:00:00Z"))
            .build());

    ResponseEntity<ExamOutput> response =
        postExam(
            validRequest(course.getId(), "0.5", "2024-2025", "2024-01-15T09:00:00Z"), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  }

  @Test
  void null_course_id_is_unprocessable() {
    ResponseEntity<String> response =
        postExamError(validRequest(null, "0.5", "2024-2025", "2024-01-15T09:00:00Z"), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("courseId is required and cannot be blank");
  }

  @Test
  void null_coefficient_is_unprocessable() {
    JCourse course = saveCourse();

    ResponseEntity<String> response =
        postExamError(
            validRequest(course.getId(), null, "2024-2025", "2024-01-15T09:00:00Z"), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("coefficient is required and cannot be blank");
  }

  @Test
  void zero_coefficient_is_unprocessable() {
    JCourse course = saveCourse();

    ResponseEntity<String> response =
        postExamError(
            validRequest(course.getId(), "0", "2024-2025", "2024-01-15T09:00:00Z"), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("coefficient must be greater than 0");
  }

  @Test
  void coefficient_over_one_is_unprocessable() {
    JCourse course = saveCourse();

    ResponseEntity<String> response =
        postExamError(
            validRequest(course.getId(), "1.5", "2024-2025", "2024-01-15T09:00:00Z"), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("coefficient must not exceed 1");
  }

  @Test
  void coefficient_beyond_two_decimals_is_unprocessable() {
    JCourse course = saveCourse();

    ResponseEntity<String> response =
        postExamError(
            validRequest(course.getId(), "0.333", "2024-2025", "2024-01-15T09:00:00Z"),
            adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("coefficient must have at most 2 decimal places");
  }

  @Test
  void null_academic_year_is_unprocessable() {
    JCourse course = saveCourse();

    ResponseEntity<String> response =
        postExamError(
            validRequest(course.getId(), "0.5", null, "2024-01-15T09:00:00Z"), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("academicYear is required and cannot be blank");
  }

  @Test
  void invalid_academic_year_is_unprocessable() {
    JCourse course = saveCourse();

    ResponseEntity<String> response =
        postExamError(
            validRequest(course.getId(), "0.5", "2024-25", "2024-01-15T09:00:00Z"), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody())
        .contains("academicYear is invalid, it must follow the format YYYY-YYYY");
  }

  @Test
  void null_date_is_unprocessable() {
    JCourse course = saveCourse();

    ResponseEntity<String> response =
        postExamError(validRequest(course.getId(), "0.5", "2024-2025", null), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("date is required and cannot be blank");
  }

  @Test
  void malformed_body_is_bad_request() {
    HttpHeaders headers = jsonHeaders(adminToken());

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/exams", HttpMethod.POST, new HttpEntity<>("{not-json", headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("Request body is missing or malformed.");
  }

  @Test
  void invalid_uuid_in_body_is_bad_request() {
    HttpHeaders headers = jsonHeaders(adminToken());
    String body =
        "{\"courseId\":\"not-a-uuid\",\"coefficient\":0.5,\"academicYear\":\"2024-2025\",\"date\":"
            + "\"2024-01-15T09:00:00Z\"}";

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/exams", HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("Request body is missing or malformed.");
  }

  private JCourse saveCourse() {
    return courseRepository.save(
        JCourse.builder().ref("EP1").title("Digital Electronics").credits(6).build());
  }

  private CreateExamInput validRequest(
      UUID courseId, String coefficient, String academicYear, String date) {
    return new CreateExamInput(
        courseId,
        coefficient == null ? null : new BigDecimal(coefficient),
        academicYear,
        date == null ? null : Instant.parse(date));
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

  private JUser saveTeacherUser(JTeacherInheritance inheritance) {
    return authRepository.save(
        JUser.builder()
            .username("terry_teacher")
            .password(passwordEncoder.encode("StrongPass12!"))
            .firstName("Some")
            .lastName("User")
            .email("terry.teacher@hacheuil.edu")
            .role(UserRole.TEACHER)
            .teacherInheritance(inheritance)
            .build());
  }

  private String teacherTokenWithCourse(JCourse course) {
    JTeacherInheritance inheritance =
        teacherInheritanceRepository.save(
            JTeacherInheritance.builder().ref("T001").teacherStatus(TeacherStatus.ACTIVE).build());
    teacherCourseRepository.save(
        JTeacherCourse.builder()
            .teacherInheritance(inheritance)
            .courseId(course.getId())
            .assignedAt(Instant.parse("2023-09-01T08:00:00Z"))
            .build());
    JUser teacher = saveTeacherUser(inheritance);
    return jwtTokenProvider.generateToken(teacher.getId().toString(), teacher.getRole().name());
  }

  private String teacherTokenWithoutCourse() {
    JTeacherInheritance inheritance =
        teacherInheritanceRepository.save(
            JTeacherInheritance.builder().ref("T001").teacherStatus(TeacherStatus.ACTIVE).build());
    JUser teacher = saveTeacherUser(inheritance);
    return jwtTokenProvider.generateToken(teacher.getId().toString(), teacher.getRole().name());
  }

  private String studentToken() {
    JUser student = saveUser("sam_student", "sam.student@hacheuil.edu", UserRole.STUDENT);
    return jwtTokenProvider.generateToken(student.getId().toString(), student.getRole().name());
  }

  private ResponseEntity<ExamOutput> postExam(CreateExamInput request, String token) {
    return restTemplate.exchange(
        "/exams", HttpMethod.POST, new HttpEntity<>(request, jsonHeaders(token)), ExamOutput.class);
  }

  private ResponseEntity<String> postExamError(CreateExamInput request, String token) {
    return restTemplate.exchange(
        "/exams", HttpMethod.POST, new HttpEntity<>(request, jsonHeaders(token)), String.class);
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
