package com.techindna.anerti.endpoint.rest.controller.courses;

import static org.assertj.core.api.Assertions.assertThat;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.dto.CourseOutput;
import com.techindna.anerti.dto.CreateCourseInput;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.CourseRepository;
import com.techindna.anerti.repository.enums.CourseType;
import com.techindna.anerti.repository.enums.UserRole;
import com.techindna.anerti.repository.model.JCourse;
import com.techindna.anerti.repository.model.JUser;
import com.techindna.anerti.security.jwt.JwtTokenProvider;
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
class PostCoursesIT extends FacadeIT {

  private final TestRestTemplate restTemplate;
  private final AuthRepository authRepository;
  private final CourseRepository courseRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;

  PostCoursesIT(
      TestRestTemplate restTemplate,
      AuthRepository authRepository,
      CourseRepository courseRepository,
      JwtTokenProvider jwtTokenProvider,
      PasswordEncoder passwordEncoder) {
    this.restTemplate = restTemplate;
    this.authRepository = authRepository;
    this.courseRepository = courseRepository;
    this.jwtTokenProvider = jwtTokenProvider;
    this.passwordEncoder = passwordEncoder;
    restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
  }

  @BeforeEach
  void clean() {
    authRepository.deleteAll();
    courseRepository.deleteAll();
  }

  @Test
  void admin_creates_course_with_default_type() {
    ResponseEntity<CourseOutput> response = postCourse(validRequest(), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    CourseOutput body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.ref()).isEqualTo("EP1");
    assertThat(body.title()).isEqualTo("Digital Electronics");
    assertThat(body.type()).isEqualTo(CourseType.COMMON);
    assertThat(body.credits()).isEqualTo(6);
    assertThat(body.createdAt()).isNotNull();

    JCourse saved = courseRepository.findByRef("EP1").orElseThrow();
    assertThat(saved.getTitle()).isEqualTo("Digital Electronics");
    assertThat(saved.getType()).isEqualTo(CourseType.COMMON);
    assertThat(saved.getCredits()).isEqualTo(6);
  }

  @Test
  void admin_creates_course_with_explicit_type() {
    CreateCourseInput request =
        new CreateCourseInput("EP1", "Digital Electronics", CourseType.EL, 6);

    ResponseEntity<CourseOutput> response = postCourse(request, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    CourseOutput body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.type()).isEqualTo(CourseType.EL);
  }

  @Test
  void missing_token_is_unauthorized() {
    ResponseEntity<String> response = postCourseError(validRequest(), null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("Authentication required.");
  }

  @Test
  void teacher_role_is_forbidden() {
    ResponseEntity<String> response = postCourseError(validRequest(), teacherToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Insufficient privileges.");
  }

  @Test
  void student_role_is_forbidden() {
    ResponseEntity<String> response = postCourseError(validRequest(), studentToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Insufficient privileges.");
  }

  @Test
  void duplicate_ref_is_conflict() {
    courseRepository.save(
        JCourse.builder().ref("EP1").title("Digital Electronics").credits(6).build());

    ResponseEntity<String> response = postCourseError(validRequest(), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).contains("Cannot use ref EP1");
  }

  @Test
  void blank_ref_is_unprocessable() {
    CreateCourseInput request = withRef(validRequest(), " ");

    ResponseEntity<String> response = postCourseError(request, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("ref is required and cannot be blank");
  }

  @Test
  void invalid_ref_is_unprocessable() {
    CreateCourseInput request = withRef(validRequest(), "EP 1");

    ResponseEntity<String> response = postCourseError(request, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("Ref EP 1 is invalid");
  }

  @Test
  void blank_title_is_unprocessable() {
    CreateCourseInput request = withTitle(validRequest(), "");

    ResponseEntity<String> response = postCourseError(request, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("title is required and cannot be blank");
  }

  @Test
  void credits_below_range_is_unprocessable() {
    CreateCourseInput request = withCredits(validRequest(), 0);

    ResponseEntity<String> response = postCourseError(request, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("credits must be between 1 and 30");
  }

  @Test
  void credits_above_range_is_unprocessable() {
    CreateCourseInput request = withCredits(validRequest(), 31);

    ResponseEntity<String> response = postCourseError(request, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("credits must be between 1 and 30");
  }

  @Test
  void missing_credits_is_unprocessable() {
    CreateCourseInput request = withCredits(validRequest(), null);

    ResponseEntity<String> response = postCourseError(request, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("credits is required and cannot be blank");
  }

  @Test
  void malformed_body_is_bad_request() {
    HttpHeaders headers = jsonHeaders(adminToken());

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/courses", HttpMethod.POST, new HttpEntity<>("{not-json", headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("Request body is missing or malformed.");
  }

  private CreateCourseInput validRequest() {
    return new CreateCourseInput("EP1", "Digital Electronics", null, 6);
  }

  private CreateCourseInput withRef(CreateCourseInput request, String ref) {
    return new CreateCourseInput(ref, request.title(), request.type(), request.credits());
  }

  private CreateCourseInput withTitle(CreateCourseInput request, String title) {
    return new CreateCourseInput(request.ref(), title, request.type(), request.credits());
  }

  private CreateCourseInput withCredits(CreateCourseInput request, Integer credits) {
    return new CreateCourseInput(request.ref(), request.title(), request.type(), credits);
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

  private ResponseEntity<CourseOutput> postCourse(CreateCourseInput request, String token) {
    return restTemplate.exchange(
        "/courses",
        HttpMethod.POST,
        new HttpEntity<>(request, jsonHeaders(token)),
        CourseOutput.class);
  }

  private ResponseEntity<String> postCourseError(CreateCourseInput request, String token) {
    return restTemplate.exchange(
        "/courses", HttpMethod.POST, new HttpEntity<>(request, jsonHeaders(token)), String.class);
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
