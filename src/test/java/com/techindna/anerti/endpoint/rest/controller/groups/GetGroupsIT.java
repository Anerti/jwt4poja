package com.techindna.anerti.endpoint.rest.controller.groups;

import static org.assertj.core.api.Assertions.assertThat;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.dto.GroupListResponse;
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
class GetGroupsIT extends FacadeIT {

  private final TestRestTemplate restTemplate;
  private final AuthRepository authRepository;
  private final GroupRepository groupRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;

  GetGroupsIT(
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
  void admin_lists_all_groups_with_meta() {
    saveGroup("CQ1", CourseType.COMMON);
    saveGroup("TSMA1", CourseType.TN);
    saveGroup("CQ2", CourseType.EL);

    ResponseEntity<GroupListResponse> response = getGroups("", adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    GroupListResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.meta().page()).isEqualTo(1);
    assertThat(body.meta().size()).isEqualTo(10);
    assertThat(body.meta().total()).isEqualTo(3);
    assertThat(body.data()).hasSize(3);
    assertThat(body.data())
        .extracting(GroupOutput::ref)
        .containsExactlyInAnyOrder("CQ1", "TSMA1", "CQ2");
  }

  @Test
  void teacher_can_list_groups() {
    saveGroup("CQ1", CourseType.COMMON);

    ResponseEntity<GroupListResponse> response = getGroups("", teacherToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().meta().total()).isEqualTo(1);
  }

  @Test
  void missing_token_is_unauthorized() {
    ResponseEntity<String> response = getGroupsError("", null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("Authentication required.");
  }

  @Test
  void student_role_is_forbidden() {
    saveGroup("CQ1", CourseType.COMMON);

    ResponseEntity<String> response = getGroupsError("", studentToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Insufficient privileges.");
  }

  @Test
  void search_matches_ref_case_insensitive() {
    saveGroup("CQ1", CourseType.COMMON);
    saveGroup("CQ2", CourseType.EL);
    saveGroup("TSMA1", CourseType.TN);
    String admin = adminToken();

    GroupListResponse byPartial = getGroups("?search=cq", admin).getBody();
    assertThat(byPartial).isNotNull();
    assertThat(byPartial.data()).hasSize(2);
    assertThat(byPartial.data())
        .extracting(GroupOutput::ref)
        .containsExactlyInAnyOrder("CQ1", "CQ2");

    GroupListResponse byExact = getGroups("?search=TSMA1", admin).getBody();
    assertThat(byExact).isNotNull();
    assertThat(byExact.data()).hasSize(1);
    assertThat(byExact.data().get(0).ref()).isEqualTo("TSMA1");
  }

  @Test
  void type_filters_exactly() {
    saveGroup("CQ1", CourseType.COMMON);
    saveGroup("TSMA1", CourseType.TN);
    saveGroup("EL1", CourseType.EL);

    GroupListResponse response = getGroups("?type=TN", adminToken()).getBody();

    assertThat(response).isNotNull();
    assertThat(response.meta().total()).isEqualTo(1);
    assertThat(response.data()).hasSize(1);
    assertThat(response.data().get(0).ref()).isEqualTo("TSMA1");
  }

  @Test
  void search_and_type_combine() {
    saveGroup("CQ1", CourseType.COMMON);
    saveGroup("CQ2", CourseType.EL);
    saveGroup("TSMA1", CourseType.TN);

    GroupListResponse response = getGroups("?search=cq&type=EL", adminToken()).getBody();

    assertThat(response).isNotNull();
    assertThat(response.meta().total()).isEqualTo(1);
    assertThat(response.data()).hasSize(1);
    assertThat(response.data().get(0).ref()).isEqualTo("CQ2");
  }

  @Test
  void pagination_is_honored() {
    saveGroup("CQ1", CourseType.COMMON);
    saveGroup("CQ2", CourseType.EL);
    saveGroup("TSMA1", CourseType.TN);
    saveGroup("TSMA2", CourseType.TN);
    saveGroup("EL1", CourseType.EL);
    String admin = adminToken();

    GroupListResponse firstPage = getGroups("?page=1&size=2", admin).getBody();
    assertThat(firstPage).isNotNull();
    assertThat(firstPage.data()).hasSize(2);
    assertThat(firstPage.meta().page()).isEqualTo(1);
    assertThat(firstPage.meta().size()).isEqualTo(2);
    assertThat(firstPage.meta().total()).isEqualTo(5);

    GroupListResponse secondPage = getGroups("?page=2&size=2", admin).getBody();
    assertThat(secondPage).isNotNull();
    assertThat(secondPage.data()).hasSize(2);
    assertThat(secondPage.meta().page()).isEqualTo(2);
    assertThat(secondPage.meta().total()).isEqualTo(5);
    assertThat(firstPage.data().get(0).ref()).isNotEqualTo(secondPage.data().get(0).ref());

    GroupListResponse all = getGroups("?size=100", admin).getBody();
    assertThat(all).isNotNull();
    assertThat(all.data()).hasSize(5);
    assertThat(all.meta().total()).isEqualTo(5);
  }

  @Test
  void invalid_page_or_size_falls_back_to_defaults() {
    saveGroup("CQ1", CourseType.COMMON);
    saveGroup("CQ2", CourseType.EL);
    saveGroup("TSMA1", CourseType.TN);
    saveGroup("TSMA2", CourseType.TN);
    saveGroup("EL1", CourseType.EL);
    String admin = adminToken();

    GroupListResponse negative = getGroups("?page=-3&size=-1", admin).getBody();
    assertThat(negative).isNotNull();
    assertThat(negative.meta().page()).isEqualTo(1);
    assertThat(negative.meta().size()).isEqualTo(10);
    assertThat(negative.data()).hasSize(5);

    GroupListResponse zero = getGroups("?page=0&size=0", admin).getBody();
    assertThat(zero).isNotNull();
    assertThat(zero.meta().page()).isEqualTo(1);
    assertThat(zero.meta().size()).isEqualTo(10);

    GroupListResponse tooLarge = getGroups("?page=101&size=101", admin).getBody();
    assertThat(tooLarge).isNotNull();
    assertThat(tooLarge.meta().page()).isEqualTo(1);
    assertThat(tooLarge.meta().size()).isEqualTo(10);
    assertThat(tooLarge.data()).hasSize(5);
  }

  @Test
  void invalid_type_is_bad_request() {
    ResponseEntity<String> response = getGroupsError("?type=NOPE", adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("Invalid parameter: type");
  }

  @Test
  void invalid_search_chars_are_unprocessable() {
    ResponseEntity<String> response = getGroupsError("?search=cq1!", adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("Search cq1! is invalid");
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

  private ResponseEntity<GroupListResponse> getGroups(String query, String token) {
    return restTemplate.exchange(
        "/groups" + query,
        HttpMethod.GET,
        new HttpEntity<>(jsonHeaders(token)),
        GroupListResponse.class);
  }

  private ResponseEntity<String> getGroupsError(String query, String token) {
    return restTemplate.exchange(
        "/groups" + query, HttpMethod.GET, new HttpEntity<>(jsonHeaders(token)), String.class);
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
