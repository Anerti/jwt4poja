package com.techindna.anerti.endpoint.rest.controller.classes;

import static org.assertj.core.api.Assertions.assertThat;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.dto.ClassListResponse;
import com.techindna.anerti.dto.ClassOutput;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.ClassRepository;
import com.techindna.anerti.repository.enums.UserRole;
import com.techindna.anerti.repository.model.JClass;
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
class GetClassesIT extends FacadeIT {

  private final TestRestTemplate restTemplate;
  private final AuthRepository authRepository;
  private final ClassRepository classRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;

  GetClassesIT(
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
  void missing_token_is_unauthorized() {
    ResponseEntity<String> response = getClassesError("", null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("Authentication required.");
  }

  @Test
  void student_is_forbidden() {
    ResponseEntity<String> response = getClassesError("", studentToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void admin_lists_all_classes() {
    saveClass("L2-EL-2024", 2024);
    saveClass("L2-TN-2024", 2024);
    saveClass("L2-EL-2023", 2023);

    ResponseEntity<ClassListResponse> response = getClasses("", adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    ClassListResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.meta().total()).isEqualTo(3);
    assertThat(body.data()).hasSize(3);
  }

  @Test
  void teacher_lists_all_classes() {
    saveClass("L2-EL-2024", 2024);

    ResponseEntity<ClassListResponse> response = getClasses("", teacherToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    ClassListResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.meta().total()).isEqualTo(1);
  }

  @Test
  void search_filters_by_name_partial_match() {
    saveClass("L2-EL-2024", 2024);
    saveClass("L2-TN-2024", 2024);
    saveClass("L3-EL-2024", 2024);

    ClassListResponse response = getClasses("?search=EL", adminToken()).getBody();

    assertThat(response).isNotNull();
    assertThat(response.meta().total()).isEqualTo(2);
    assertThat(response.data()).extracting(ClassOutput::name).allMatch(name -> name.contains("EL"));
  }

  @Test
  void search_is_case_insensitive() {
    saveClass("L2-EL-2024", 2024);

    ClassListResponse response = getClasses("?search=el", adminToken()).getBody();

    assertThat(response).isNotNull();
    assertThat(response.meta().total()).isEqualTo(1);
  }

  @Test
  void pagination_is_honored() {
    for (int i = 0; i < 5; i++) {
      saveClass("CLASS" + i, 2020 + i);
    }

    ClassListResponse firstPage = getClasses("?page=1&size=2", adminToken()).getBody();
    assertThat(firstPage).isNotNull();
    assertThat(firstPage.data()).hasSize(2);
    assertThat(firstPage.meta().page()).isEqualTo(1);
    assertThat(firstPage.meta().size()).isEqualTo(2);
    assertThat(firstPage.meta().total()).isEqualTo(5);

    ClassListResponse secondPage = getClasses("?page=2&size=2", adminToken()).getBody();
    assertThat(secondPage).isNotNull();
    assertThat(secondPage.data()).hasSize(2);
    assertThat(secondPage.meta().page()).isEqualTo(2);
  }

  @Test
  void invalid_page_or_size_falls_back_to_defaults() {
    saveClass("L2-EL-2024", 2024);

    ClassListResponse negative = getClasses("?page=-3&size=-1", adminToken()).getBody();
    assertThat(negative).isNotNull();
    assertThat(negative.meta().page()).isEqualTo(1);
    assertThat(negative.meta().size()).isEqualTo(10);
    assertThat(negative.data()).hasSize(1);
  }

  @Test
  void empty_list_returns_zero_total() {
    ClassListResponse response = getClasses("", adminToken()).getBody();

    assertThat(response).isNotNull();
    assertThat(response.meta().total()).isZero();
    assertThat(response.data()).isEmpty();
  }

  private JClass saveClass(String name, int yearOf) {
    return classRepository.save(JClass.builder().name(name).yearOf(yearOf).build());
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

  private ResponseEntity<ClassListResponse> getClasses(String query, String token) {
    return restTemplate.exchange(
        "/classes" + query,
        HttpMethod.GET,
        new HttpEntity<>(jsonHeaders(token)),
        ClassListResponse.class);
  }

  private ResponseEntity<String> getClassesError(String query, String token) {
    return restTemplate.exchange(
        "/classes" + query, HttpMethod.GET, new HttpEntity<>(jsonHeaders(token)), String.class);
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
