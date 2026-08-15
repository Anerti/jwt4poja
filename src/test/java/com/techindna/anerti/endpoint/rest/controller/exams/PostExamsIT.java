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
  private final TeacherInheritanceRepository teacherInheritanceRepository;
  private final CourseRepository courseRepository;
  private final TeacherCourseRepository teacherCourseRepository;
  private final ExamRepository examRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;

  PostExamsIT(
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
  void admin_creates_exam() {
    JCourse course = saveCourse("EP1");

    ResponseEntity<ExamOutput> response = postExam(validRequest(course.getId()), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    ExamOutput body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.courseId()).isEqualTo(course.getId());
    assertThat(body.coefficient()).isEqualByComparingTo(new BigDecimal("0.5"));
    assertThat(body.academicYear()).isEqualTo(2023);
    assertThat(body.date()).isEqualTo(Instant.parse("2024-01-15T09:00:00Z"));
    assertThat(body.createdAt()).isNotNull();

    JExam saved = examRepository.findAll().getFirst();
    assertThat(saved.getCourse().getId()).isEqualTo(course.getId());
  }

  @Test
  void teacher_creates_exam_for_assigned_course() {
    JTeacherInheritance teacher = saveTeacher();
    JCourse course = saveCourse("EP1");
    assign(teacher, course);

    ResponseEntity<ExamOutput> response =
        postExam(validRequest(course.getId()), teacherToken(teacher));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().courseId()).isEqualTo(course.getId());
  }

  @Test
  void teacher_cannot_create_exam_for_unassigned_course() {
    JTeacherInheritance teacher = saveTeacher();
    JCourse course = saveCourse("EP1");

    ResponseEntity<String> response =
        postExamError(validRequest(course.getId()), teacherToken(teacher));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody())
        .contains(
            "Teacher %s is not assigned to course %s".formatted(teacher.getId(), course.getId()));
  }

  @Test
  void missing_token_is_unauthorized() {
    JCourse course = saveCourse("EP1");

    ResponseEntity<String> response = postExamError(validRequest(course.getId()), null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("Authentication required.");
  }

  @Test
  void student_role_is_forbidden() {
    JCourse course = saveCourse("EP1");

    ResponseEntity<String> response = postExamError(validRequest(course.getId()), studentToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Insufficient privileges.");
  }

  @Test
  void unknown_course_is_not_found() {
    UUID unknownCourse = UUID.randomUUID();

    ResponseEntity<String> response = postExamError(validRequest(unknownCourse), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).contains("Course %s not found".formatted(unknownCourse));
  }

  @Test
  void blank_coefficient_is_unprocessable() {
    JCourse course = saveCourse("EP1");

    ResponseEntity<String> response =
        postExamError(withCoefficient(validRequest(course.getId()), null), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("coefficient is required and cannot be blank");
  }

  @Test
  void zero_coefficient_is_unprocessable() {
    JCourse course = saveCourse("EP1");

    ResponseEntity<String> response =
        postExamError(withCoefficient(validRequest(course.getId()), BigDecimal.ZERO), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("coefficient must be greater than 0");
  }

  @Test
  void missing_academic_year_is_unprocessable() {
    JCourse course = saveCourse("EP1");

    ResponseEntity<String> response =
        postExamError(withAcademicYear(validRequest(course.getId()), null), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("academicYear is required and cannot be blank");
  }

  @Test
  void missing_date_is_unprocessable() {
    JCourse course = saveCourse("EP1");

    ResponseEntity<String> response =
        postExamError(withDate(validRequest(course.getId()), null), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("date is required and cannot be blank");
  }

  private CreateExamInput validRequest(UUID courseId) {
    return new CreateExamInput(
        courseId, new BigDecimal("0.5"), 2023, Instant.parse("2024-01-15T09:00:00Z"));
  }

  private CreateExamInput withCoefficient(CreateExamInput request, BigDecimal coefficient) {
    return new CreateExamInput(
        request.courseId(), coefficient, request.academicYear(), request.date());
  }

  private CreateExamInput withAcademicYear(CreateExamInput request, Integer academicYear) {
    return new CreateExamInput(
        request.courseId(), request.coefficient(), academicYear, request.date());
  }

  private CreateExamInput withDate(CreateExamInput request, Instant date) {
    return new CreateExamInput(
        request.courseId(), request.coefficient(), request.academicYear(), date);
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
