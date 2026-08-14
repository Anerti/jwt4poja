package com.techindna.anerti.endpoint.rest.controller.groups;

import static org.assertj.core.api.Assertions.assertThat;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.GroupRepository;
import com.techindna.anerti.repository.enums.CourseType;
import com.techindna.anerti.repository.enums.UserRole;
import com.techindna.anerti.repository.model.JGroup;
import com.techindna.anerti.repository.model.JUser;
import com.techindna.anerti.security.jwt.JwtTokenProvider;
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
class DeleteGroupsIT extends FacadeIT {

  private final TestRestTemplate restTemplate;
  private final AuthRepository authRepository;
  private final GroupRepository groupRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;

  DeleteGroupsIT(
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
  void admin_deletes_an_existing_group() {
    UUID groupId = saveGroup("CQ1", CourseType.COMMON).getId();

    ResponseEntity<String> response = deleteGroup(groupId, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(groupRepository.findById(groupId)).isEmpty();
  }

  @Test
  void missing_token_is_unauthorized() {
    UUID groupId = saveGroup("CQ1", CourseType.COMMON).getId();

    ResponseEntity<String> response = deleteGroup(groupId, null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("Authentication required.");
  }

  @Test
  void teacher_role_is_forbidden() {
    UUID groupId = saveGroup("CQ1", CourseType.COMMON).getId();

    ResponseEntity<String> response = deleteGroup(groupId, teacherToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Insufficient privileges.");
  }

  @Test
  void student_role_is_forbidden() {
    UUID groupId = saveGroup("CQ1", CourseType.COMMON).getId();

    ResponseEntity<String> response = deleteGroup(groupId, studentToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Insufficient privileges.");
  }

  @Test
  void unknown_group_id_is_not_found() {
    ResponseEntity<String> response = deleteGroup(UUID.randomUUID(), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).contains("Group");
    assertThat(response.getBody()).contains("not found");
  }

  @Test
  void malformed_uuid_is_bad_request() {
    ResponseEntity<String> response = deleteGroup("not-a-uuid", adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("Invalid parameter: groupId");
  }

  private JGroup saveGroup(String ref, CourseType type) {
    return groupRepository.save(JGroup.builder().ref(ref).type(type).build());
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

  private ResponseEntity<String> deleteGroup(UUID groupId, String token) {
    return deleteGroup(groupId.toString(), token);
  }

  private ResponseEntity<String> deleteGroup(String groupId, String token) {
    return restTemplate.exchange(
        "/groups/" + groupId,
        HttpMethod.DELETE,
        new HttpEntity<>(jsonHeaders(token)),
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
