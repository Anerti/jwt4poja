package com.techindna.anerti.endpoint.rest.controller.classes;

import static org.assertj.core.api.Assertions.assertThat;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.dto.ClassOutput;
import com.techindna.anerti.dto.CreateClassInput;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.ClassRepository;
import com.techindna.anerti.repository.enums.UserRole;
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
class PostClassesIT extends FacadeIT {

  private final TestRestTemplate restTemplate;
  private final AuthRepository authRepository;
  private final ClassRepository classRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;

  PostClassesIT(
      TestRestTemplate restTemplate,
      AuthRepository authRepository,
      ClassRepository classRepository,
      JwtTokenProvider jwtTokenProvider,
      PasswordEncoder passwordEncoder) {
    this.restTemplate = restTemplate;
    this.authRepository = authRepository;
    this.classRepository = classRepository;
    this.jwtTokenProvider = jwtTokenProvider;
    this.passwordEncoder = passwordEncoder;
    restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
  }

  @BeforeEach
  void clean() {
    classRepository.deleteAll();
    authRepository.deleteAll();
  }

  @Test
  void admin_creates_class() {
    ResponseEntity<ClassOutput> response =
        postClass(new CreateClassInput("L2-EL-2024", 2024), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    ClassOutput body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.name()).isEqualTo("L2-EL-2024");
    assertThat(body.yearOf()).isEqualTo(2024);
    assertThat(body.id()).isNotNull();
    assertThat(body.createdAt()).isNotNull();
  }

  @Test
  void teacher_creates_class_is_forbidden() {
    ResponseEntity<String> response =
        postClassError(new CreateClassInput("L2-EL-2024", 2024), teacherToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Insufficient privileges.");
  }

  @Test
  void student_creates_class_is_forbidden() {
    ResponseEntity<String> response =
        postClassError(new CreateClassInput("L2-EL-2024", 2024), studentToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Insufficient privileges.");
  }

  @Test
  void missing_token_is_unauthorized() {
    ResponseEntity<String> response =
        postClassError(new CreateClassInput("L2-EL-2024", 2024), null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("Authentication required.");
  }

  @Test
  void duplicate_name_is_conflict() {
    classRepository.save(
        com.techindna.anerti.repository.model.JClass.builder()
            .name("L2-EL-2024")
            .yearOf(2024)
            .build());

    ResponseEntity<String> response =
        postClassError(new CreateClassInput("L2-EL-2024", 2025), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).contains("Class L2-EL-2024 already exists");
  }

  @Test
  void null_name_is_unprocessable() {
    ResponseEntity<String> response =
        postClassError(new CreateClassInput(null, 2024), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("name is required and cannot be blank");
  }

  @Test
  void blank_name_is_unprocessable() {
    ResponseEntity<String> response =
        postClassError(new CreateClassInput("  ", 2024), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("name is required and cannot be blank");
  }

  @Test
  void null_yearOf_is_unprocessable() {
    ResponseEntity<String> response =
        postClassError(new CreateClassInput("L2-EL-2024", null), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("yearOf is required and cannot be blank");
  }

  @Test
  void yearOf_too_low_is_bad_request() {
    ResponseEntity<String> response =
        postClassError(new CreateClassInput("L2-EL-2024", 1999), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("yearOf must be between 2000 and 2100");
  }

  @Test
  void yearOf_too_high_is_bad_request() {
    ResponseEntity<String> response =
        postClassError(new CreateClassInput("L2-EL-2024", 2101), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("yearOf must be between 2000 and 2100");
  }

  @Test
  void name_too_long_is_unprocessable() {
    ResponseEntity<String> response =
        postClassError(new CreateClassInput("A".repeat(31), 2024), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("name must not exceed 30 characters");
  }

  @Test
  void name_is_trimmed() {
    ResponseEntity<ClassOutput> response =
        postClass(new CreateClassInput("  L2-EL-2024  ", 2024), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().name()).isEqualTo("L2-EL-2024");
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

  private ResponseEntity<ClassOutput> postClass(CreateClassInput request, String token) {
    return restTemplate.exchange(
        "/classes",
        HttpMethod.POST,
        new HttpEntity<>(request, jsonHeaders(token)),
        ClassOutput.class);
  }

  private ResponseEntity<String> postClassError(CreateClassInput request, String token) {
    return restTemplate.exchange(
        "/classes", HttpMethod.POST, new HttpEntity<>(request, jsonHeaders(token)), String.class);
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
