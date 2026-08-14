package com.techindna.anerti.endpoint.rest.controller.teachers;

import static org.assertj.core.api.Assertions.assertThat;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.dto.CreateTeacherInput;
import com.techindna.anerti.dto.UserExtendTeacher;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.TeacherInheritanceRepository;
import com.techindna.anerti.repository.enums.TeacherStatus;
import com.techindna.anerti.repository.enums.UserRole;
import com.techindna.anerti.repository.model.JTeacherInheritance;
import com.techindna.anerti.repository.model.JUser;
import com.techindna.anerti.security.jwt.JwtTokenProvider;
import java.time.Instant;
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
class PostTeachersIT extends FacadeIT {

  private final TestRestTemplate restTemplate;
  private final AuthRepository authRepository;
  private final TeacherInheritanceRepository teacherInheritanceRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;

  PostTeachersIT(
      TestRestTemplate restTemplate,
      AuthRepository authRepository,
      TeacherInheritanceRepository teacherInheritanceRepository,
      JwtTokenProvider jwtTokenProvider,
      PasswordEncoder passwordEncoder) {
    this.restTemplate = restTemplate;
    this.authRepository = authRepository;
    this.teacherInheritanceRepository = teacherInheritanceRepository;
    this.jwtTokenProvider = jwtTokenProvider;
    this.passwordEncoder = passwordEncoder;
    restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
  }

  @BeforeEach
  void clean() {
    authRepository.deleteAll();
    teacherInheritanceRepository.deleteAll();
  }

  @Test
  void admin_creates_teacher_with_defaults() {
    ResponseEntity<UserExtendTeacher> response = postTeacher(validRequest(), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    UserExtendTeacher body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.username()).isEqualTo("p_martin");
    assertThat(body.firstName()).isEqualTo("Paul");
    assertThat(body.lastName()).isEqualTo("Martin");
    assertThat(body.email()).isEqualTo("paul.martin@hacheuil.edu");
    assertThat(body.role()).isEqualTo(UserRole.TEACHER);
    assertThat(body.teacherInheritance().ref()).isEqualTo("T001");
    assertThat(body.teacherInheritance().teacherStatus()).isEqualTo(TeacherStatus.ACTIVE);
    assertThat(body.teacherInheritance().joinedAt()).isNull();

    JUser saved = authRepository.findByEmail("paul.martin@hacheuil.edu").orElseThrow();
    assertThat(saved.getRole()).isEqualTo(UserRole.TEACHER);
    assertThat(saved.getTeacherInheritance().getRef()).isEqualTo("T001");
    assertThat(saved.getTeacherInheritance().getTeacherStatus()).isEqualTo(TeacherStatus.ACTIVE);
    assertThat(passwordEncoder.matches("StrongPass12!", saved.getPassword())).isTrue();
  }

  @Test
  void admin_creates_teacher_with_joined_at_and_status() {
    Instant joinedAt = Instant.parse("2023-09-01T08:00:00Z");
    CreateTeacherInput request =
        new CreateTeacherInput(
            "j_leclerc",
            "StrongPass12!",
            "Jean",
            "Leclerc",
            "jean.leclerc@hacheuil.edu",
            "T002",
            joinedAt,
            TeacherStatus.INACTIVE);

    ResponseEntity<UserExtendTeacher> response = postTeacher(request, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    UserExtendTeacher body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.teacherInheritance().ref()).isEqualTo("T002");
    assertThat(body.teacherInheritance().teacherStatus()).isEqualTo(TeacherStatus.INACTIVE);
    assertThat(body.teacherInheritance().joinedAt()).isEqualTo(joinedAt);
  }

  @Test
  void missing_token_is_unauthorized() {
    ResponseEntity<String> response = postTeacherError(validRequest(), null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("Authentication required.");
  }

  @Test
  void teacher_role_is_forbidden() {
    ResponseEntity<String> response = postTeacherError(validRequest(), teacherToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Insufficient privileges.");
  }

  @Test
  void student_role_is_forbidden() {
    ResponseEntity<String> response = postTeacherError(validRequest(), studentToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Insufficient privileges.");
  }

  @Test
  void duplicate_username_is_conflict() {
    saveUser("p_martin", "other@hacheuil.edu", UserRole.TEACHER);

    ResponseEntity<String> response = postTeacherError(validRequest(), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).contains("Cannot use username p_martin");
  }

  @Test
  void duplicate_email_is_conflict() {
    saveUser("other_user", "paul.martin@hacheuil.edu", UserRole.TEACHER);

    ResponseEntity<String> response = postTeacherError(validRequest(), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).contains("cannot use email paul.martin@hacheuil.edu");
  }

  @Test
  void duplicate_ref_is_conflict() {
    teacherInheritanceRepository.save(
        JTeacherInheritance.builder().ref("T001").teacherStatus(TeacherStatus.ACTIVE).build());

    ResponseEntity<String> response = postTeacherError(validRequest(), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).contains("Cannot use ref T001");
  }

  @Test
  void weak_password_is_unprocessable() {
    CreateTeacherInput request = withPassword(validRequest(), "short");

    ResponseEntity<String> response = postTeacherError(request, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("Password must be at least 12 characters");
  }

  @Test
  void invalid_email_is_unprocessable() {
    CreateTeacherInput request = withEmail(validRequest(), "not-an-email");

    ResponseEntity<String> response = postTeacherError(request, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("Email not-an-email is not valid");
  }

  @Test
  void blank_first_name_is_unprocessable() {
    CreateTeacherInput request = withFirstName(validRequest(), "");

    ResponseEntity<String> response = postTeacherError(request, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("firstName is required and cannot be blank");
  }

  @Test
  void invalid_ref_is_unprocessable() {
    CreateTeacherInput request = withRef(validRequest(), "T 001");

    ResponseEntity<String> response = postTeacherError(request, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("Ref T 001 is invalid");
  }

  @Test
  void malformed_body_is_bad_request() {
    HttpHeaders headers = jsonHeaders(adminToken());

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/teachers", HttpMethod.POST, new HttpEntity<>("{not-json", headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("Request body is missing or malformed.");
  }

  private CreateTeacherInput validRequest() {
    return new CreateTeacherInput(
        "p_martin",
        "StrongPass12!",
        "Paul",
        "Martin",
        "paul.martin@hacheuil.edu",
        "T001",
        null,
        null);
  }

  private CreateTeacherInput withPassword(CreateTeacherInput request, String password) {
    return new CreateTeacherInput(
        request.username(),
        password,
        request.firstName(),
        request.lastName(),
        request.email(),
        request.ref(),
        request.joinedAt(),
        request.teacherStatus());
  }

  private CreateTeacherInput withEmail(CreateTeacherInput request, String email) {
    return new CreateTeacherInput(
        request.username(),
        request.password(),
        request.firstName(),
        request.lastName(),
        email,
        request.ref(),
        request.joinedAt(),
        request.teacherStatus());
  }

  private CreateTeacherInput withFirstName(CreateTeacherInput request, String firstName) {
    return new CreateTeacherInput(
        request.username(),
        request.password(),
        firstName,
        request.lastName(),
        request.email(),
        request.ref(),
        request.joinedAt(),
        request.teacherStatus());
  }

  private CreateTeacherInput withRef(CreateTeacherInput request, String ref) {
    return new CreateTeacherInput(
        request.username(),
        request.password(),
        request.firstName(),
        request.lastName(),
        request.email(),
        ref,
        request.joinedAt(),
        request.teacherStatus());
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

  private ResponseEntity<UserExtendTeacher> postTeacher(CreateTeacherInput request, String token) {
    return restTemplate.exchange(
        "/teachers",
        HttpMethod.POST,
        new HttpEntity<>(request, jsonHeaders(token)),
        UserExtendTeacher.class);
  }

  private ResponseEntity<String> postTeacherError(CreateTeacherInput request, String token) {
    return restTemplate.exchange(
        "/teachers", HttpMethod.POST, new HttpEntity<>(request, jsonHeaders(token)), String.class);
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
