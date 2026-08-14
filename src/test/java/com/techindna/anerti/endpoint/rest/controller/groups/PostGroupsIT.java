package com.techindna.anerti.endpoint.rest.controller.groups;

import static org.assertj.core.api.Assertions.assertThat;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.dto.CreateGroupInput;
import com.techindna.anerti.dto.GroupOutput;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.GroupRepository;
import com.techindna.anerti.repository.enums.CourseType;
import com.techindna.anerti.repository.enums.UserRole;
import com.techindna.anerti.repository.model.JGroup;
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
class PostGroupsIT extends FacadeIT {

  private final TestRestTemplate restTemplate;
  private final AuthRepository authRepository;
  private final GroupRepository groupRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;

  PostGroupsIT(
      TestRestTemplate restTemplate,
      AuthRepository authRepository,
      GroupRepository groupRepository,
      JwtTokenProvider jwtTokenProvider,
      PasswordEncoder passwordEncoder) {
    this.restTemplate = restTemplate;
    this.authRepository = authRepository;
    this.groupRepository = groupRepository;
    this.jwtTokenProvider = jwtTokenProvider;
    this.passwordEncoder = passwordEncoder;
    restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
  }

  @BeforeEach
  void clean() {
    authRepository.deleteAll();
    groupRepository.deleteAll();
  }

  @Test
  void admin_creates_group_with_default_type() {
    ResponseEntity<GroupOutput> response = postGroup(validRequest(), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    GroupOutput body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.ref()).isEqualTo("CQ1");
    assertThat(body.type()).isEqualTo(CourseType.COMMON);
    assertThat(body.createdAt()).isNotNull();

    JGroup saved = groupRepository.findByRef("CQ1").orElseThrow();
    assertThat(saved.getType()).isEqualTo(CourseType.COMMON);
  }

  @Test
  void admin_creates_group_with_explicit_type() {
    CreateGroupInput request = new CreateGroupInput("TSMA1", CourseType.TN);

    ResponseEntity<GroupOutput> response = postGroup(request, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    GroupOutput body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.type()).isEqualTo(CourseType.TN);

    JGroup saved = groupRepository.findByRef("TSMA1").orElseThrow();
    assertThat(saved.getType()).isEqualTo(CourseType.TN);
  }

  @Test
  void missing_token_is_unauthorized() {
    ResponseEntity<String> response = postGroupError(validRequest(), null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("Authentication required.");
  }

  @Test
  void teacher_role_is_forbidden() {
    ResponseEntity<String> response = postGroupError(validRequest(), teacherToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Insufficient privileges.");
  }

  @Test
  void student_role_is_forbidden() {
    ResponseEntity<String> response = postGroupError(validRequest(), studentToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Insufficient privileges.");
  }

  @Test
  void duplicate_ref_is_conflict() {
    groupRepository.save(JGroup.builder().ref("CQ1").build());

    ResponseEntity<String> response = postGroupError(validRequest(), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).contains("Cannot use ref CQ1");
  }

  @Test
  void blank_ref_is_unprocessable() {
    CreateGroupInput request = new CreateGroupInput(" ", null);

    ResponseEntity<String> response = postGroupError(request, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("ref is required and cannot be blank");
  }

  @Test
  void invalid_ref_is_unprocessable() {
    CreateGroupInput request = new CreateGroupInput("CQ 1", null);

    ResponseEntity<String> response = postGroupError(request, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("Ref CQ 1 is invalid");
  }

  @Test
  void malformed_body_is_bad_request() {
    HttpHeaders headers = jsonHeaders(adminToken());

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/groups", HttpMethod.POST, new HttpEntity<>("{not-json", headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("Request body is missing or malformed.");
  }

  private CreateGroupInput validRequest() {
    return new CreateGroupInput("CQ1", null);
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

  private ResponseEntity<GroupOutput> postGroup(CreateGroupInput request, String token) {
    return restTemplate.exchange(
        "/groups",
        HttpMethod.POST,
        new HttpEntity<>(request, jsonHeaders(token)),
        GroupOutput.class);
  }

  private ResponseEntity<String> postGroupError(CreateGroupInput request, String token) {
    return restTemplate.exchange(
        "/groups", HttpMethod.POST, new HttpEntity<>(request, jsonHeaders(token)), String.class);
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
