package com.techindna.anerti.endpoint.rest.controller.teachers;

import static org.assertj.core.api.Assertions.assertThat;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.dto.TeacherListResponse;
import com.techindna.anerti.dto.UserExtendTeacher;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.TeacherInheritanceRepository;
import com.techindna.anerti.repository.enums.TeacherStatus;
import com.techindna.anerti.repository.enums.UserRole;
import com.techindna.anerti.repository.model.JTeacherInheritance;
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
class GetTeachersIT extends FacadeIT {

  private final TestRestTemplate restTemplate;
  private final AuthRepository authRepository;
  private final TeacherInheritanceRepository teacherInheritanceRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;

  GetTeachersIT(
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
  void admin_lists_all_teachers_with_meta() {
    saveTeacher("T001", "p_martin", "Paul", "Martin", TeacherStatus.ACTIVE);
    saveTeacher("T002", "j_leclerc", "Jean", "Leclerc", TeacherStatus.INACTIVE);

    ResponseEntity<TeacherListResponse> response = getTeachers("", adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    TeacherListResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.meta().page()).isEqualTo(1);
    assertThat(body.meta().size()).isEqualTo(10);
    assertThat(body.meta().total()).isEqualTo(2);
    assertThat(body.data()).hasSize(2);
    assertThat(body.data())
        .extracting(UserExtendTeacher::username)
        .containsExactlyInAnyOrder("p_martin", "j_leclerc");
    assertThat(body.data())
        .extracting(teacher -> teacher.teacherInheritance().ref())
        .containsExactlyInAnyOrder("T001", "T002");
  }

  @Test
  void search_matches_ref_first_name_and_last_name_case_insensitive() {
    saveTeacher("T001", "p_martin", "Paul", "Martin", TeacherStatus.ACTIVE);
    saveTeacher("T002", "j_leclerc", "Jean", "Leclerc", TeacherStatus.ACTIVE);
    String admin = adminToken();

    TeacherListResponse byFirstName = getTeachers("?search=paul", admin).getBody();
    assertThat(byFirstName).isNotNull();
    assertThat(byFirstName.data()).hasSize(1);
    assertThat(byFirstName.data().get(0).username()).isEqualTo("p_martin");

    TeacherListResponse byLastName = getTeachers("?search=LECLERC", admin).getBody();
    assertThat(byLastName).isNotNull();
    assertThat(byLastName.data()).hasSize(1);
    assertThat(byLastName.data().get(0).username()).isEqualTo("j_leclerc");

    TeacherListResponse byRef = getTeachers("?search=t002", admin).getBody();
    assertThat(byRef).isNotNull();
    assertThat(byRef.data()).hasSize(1);
    assertThat(byRef.data().get(0).username()).isEqualTo("j_leclerc");
  }

  @Test
  void teacher_status_filters_exactly() {
    saveTeacher("T001", "p_martin", "Paul", "Martin", TeacherStatus.ACTIVE);
    saveTeacher("T002", "j_leclerc", "Jean", "Leclerc", TeacherStatus.INACTIVE);
    saveTeacher("T003", "s_bernard", "Sara", "Bernard", TeacherStatus.OTHER);

    TeacherListResponse response = getTeachers("?teacherStatus=INACTIVE", adminToken()).getBody();

    assertThat(response).isNotNull();
    assertThat(response.meta().total()).isEqualTo(1);
    assertThat(response.data()).hasSize(1);
    assertThat(response.data().get(0).username()).isEqualTo("j_leclerc");
    assertThat(response.data().get(0).teacherInheritance().teacherStatus())
        .isEqualTo(TeacherStatus.INACTIVE);
  }

  @Test
  void search_and_teacher_status_combine() {
    saveTeacher("T001", "p_martin", "Paul", "Martin", TeacherStatus.ACTIVE);
    saveTeacher("T002", "p_bernard", "Paul", "Bernard", TeacherStatus.INACTIVE);
    saveTeacher("T003", "j_paul", "Jean", "Paul", TeacherStatus.ACTIVE);

    TeacherListResponse response =
        getTeachers("?search=paul&teacherStatus=ACTIVE", adminToken()).getBody();

    assertThat(response).isNotNull();
    assertThat(response.meta().total()).isEqualTo(2);
    assertThat(response.data())
        .extracting(UserExtendTeacher::username)
        .containsExactlyInAnyOrder("p_martin", "j_paul");
  }

  @Test
  void pagination_is_honored() {
    saveTeacher("T001", "p_martin", "Paul", "Martin", TeacherStatus.ACTIVE);
    saveTeacher("T002", "j_leclerc", "Jean", "Leclerc", TeacherStatus.ACTIVE);
    saveTeacher("T003", "s_bernard", "Sara", "Bernard", TeacherStatus.ACTIVE);
    saveTeacher("T004", "l_dupont", "Luc", "Dupont", TeacherStatus.ACTIVE);
    saveTeacher("T005", "a_moreau", "Alice", "Moreau", TeacherStatus.ACTIVE);
    String admin = adminToken();

    TeacherListResponse firstPage = getTeachers("?page=1&size=2", admin).getBody();
    assertThat(firstPage).isNotNull();
    assertThat(firstPage.data()).hasSize(2);
    assertThat(firstPage.meta().page()).isEqualTo(1);
    assertThat(firstPage.meta().size()).isEqualTo(2);
    assertThat(firstPage.meta().total()).isEqualTo(5);

    TeacherListResponse secondPage = getTeachers("?page=2&size=2", admin).getBody();
    assertThat(secondPage).isNotNull();
    assertThat(secondPage.data()).hasSize(2);
    assertThat(secondPage.meta().page()).isEqualTo(2);
    assertThat(secondPage.meta().total()).isEqualTo(5);
    assertThat(firstPage.data().get(0).username())
        .isNotEqualTo(secondPage.data().get(0).username());

    TeacherListResponse all = getTeachers("?size=100", admin).getBody();
    assertThat(all).isNotNull();
    assertThat(all.data()).hasSize(5);
    assertThat(all.meta().total()).isEqualTo(5);
  }

  @Test
  void invalid_page_or_size_falls_back_to_defaults() {
    saveTeacher("T001", "p_martin", "Paul", "Martin", TeacherStatus.ACTIVE);
    saveTeacher("T002", "j_leclerc", "Jean", "Leclerc", TeacherStatus.ACTIVE);
    saveTeacher("T003", "s_bernard", "Sara", "Bernard", TeacherStatus.ACTIVE);
    saveTeacher("T004", "l_dupont", "Luc", "Dupont", TeacherStatus.ACTIVE);
    saveTeacher("T005", "a_moreau", "Alice", "Moreau", TeacherStatus.ACTIVE);
    String admin = adminToken();

    TeacherListResponse negative = getTeachers("?page=-3&size=-1", admin).getBody();
    assertThat(negative).isNotNull();
    assertThat(negative.meta().page()).isEqualTo(1);
    assertThat(negative.meta().size()).isEqualTo(10);
    assertThat(negative.data()).hasSize(5);

    TeacherListResponse zero = getTeachers("?page=0&size=0", admin).getBody();
    assertThat(zero).isNotNull();
    assertThat(zero.meta().page()).isEqualTo(1);
    assertThat(zero.meta().size()).isEqualTo(10);

    TeacherListResponse tooLarge = getTeachers("?page=101&size=101", admin).getBody();
    assertThat(tooLarge).isNotNull();
    assertThat(tooLarge.meta().page()).isEqualTo(1);
    assertThat(tooLarge.meta().size()).isEqualTo(10);
    assertThat(tooLarge.data()).hasSize(5);
  }

  @Test
  void non_teacher_users_are_excluded() {
    saveTeacher("T001", "p_martin", "Paul", "Martin", TeacherStatus.ACTIVE);
    saveUser("sam_student", "Sam", "Student", UserRole.STUDENT);

    TeacherListResponse response = getTeachers("", adminToken()).getBody();

    assertThat(response).isNotNull();
    assertThat(response.meta().total()).isEqualTo(1);
    assertThat(response.data().get(0).username()).isEqualTo("p_martin");
  }

  @Test
  void missing_token_is_unauthorized() {
    ResponseEntity<String> response = getTeachersError("", null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("Authentication required.");
  }

  @Test
  void teacher_role_is_forbidden() {
    ResponseEntity<String> response = getTeachersError("", teacherToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Insufficient privileges.");
  }

  @Test
  void student_role_is_forbidden() {
    ResponseEntity<String> response = getTeachersError("", studentToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Insufficient privileges.");
  }

  @Test
  void invalid_teacher_status_is_bad_request() {
    ResponseEntity<String> response = getTeachersError("?teacherStatus=NOPE", adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("Invalid parameter: teacherStatus");
  }

  @Test
  void invalid_search_chars_are_unprocessable() {
    ResponseEntity<String> response = getTeachersError("?search=paul!", adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("Search paul! is invalid");
  }

  private JUser saveTeacher(
      String ref, String username, String firstName, String lastName, TeacherStatus status) {
    JTeacherInheritance inheritance =
        teacherInheritanceRepository.save(
            JTeacherInheritance.builder().ref(ref).teacherStatus(status).build());
    return authRepository.save(
        JUser.builder()
            .username(username)
            .password(passwordEncoder.encode("StrongPass12!"))
            .firstName(firstName)
            .lastName(lastName)
            .email(username + "@hacheuil.edu")
            .role(UserRole.TEACHER)
            .teacherInheritance(inheritance)
            .build());
  }

  private JUser saveUser(String username, String firstName, String lastName, UserRole role) {
    return authRepository.save(
        JUser.builder()
            .username(username)
            .password(passwordEncoder.encode("StrongPass12!"))
            .firstName(firstName)
            .lastName(lastName)
            .email(username + "@hacheuil.edu")
            .role(role)
            .build());
  }

  private String adminToken() {
    JUser admin = saveUser("root_admin", "Root", "Admin", UserRole.ADMIN);
    return jwtTokenProvider.generateToken(admin.getId().toString(), admin.getRole().name());
  }

  private String teacherToken() {
    JUser teacher = saveTeacher("T099", "terry_teacher", "Terry", "Teacher", TeacherStatus.ACTIVE);
    return jwtTokenProvider.generateToken(teacher.getId().toString(), teacher.getRole().name());
  }

  private String studentToken() {
    JUser student = saveUser("sam_student", "Sam", "Student", UserRole.STUDENT);
    return jwtTokenProvider.generateToken(student.getId().toString(), student.getRole().name());
  }

  private ResponseEntity<TeacherListResponse> getTeachers(String query, String token) {
    return restTemplate.exchange(
        "/teachers" + query,
        HttpMethod.GET,
        new HttpEntity<>(jsonHeaders(token)),
        TeacherListResponse.class);
  }

  private ResponseEntity<String> getTeachersError(String query, String token) {
    return restTemplate.exchange(
        "/teachers" + query, HttpMethod.GET, new HttpEntity<>(jsonHeaders(token)), String.class);
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
