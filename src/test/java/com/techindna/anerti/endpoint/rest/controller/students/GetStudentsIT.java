package com.techindna.anerti.endpoint.rest.controller.students;

import static org.assertj.core.api.Assertions.assertThat;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.dto.StudentListResponse;
import com.techindna.anerti.dto.UserExtendStudent;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.StudentInheritanceRepository;
import com.techindna.anerti.repository.enums.LearningPath;
import com.techindna.anerti.repository.enums.Level;
import com.techindna.anerti.repository.enums.StudentStatus;
import com.techindna.anerti.repository.enums.UserRole;
import com.techindna.anerti.repository.model.JStudentInheritance;
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
class GetStudentsIT extends FacadeIT {

  private final TestRestTemplate restTemplate;
  private final AuthRepository authRepository;
  private final StudentInheritanceRepository studentInheritanceRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;

  GetStudentsIT(
      TestRestTemplate restTemplate,
      AuthRepository authRepository,
      StudentInheritanceRepository studentInheritanceRepository,
      JwtTokenProvider jwtTokenProvider,
      PasswordEncoder passwordEncoder) {
    this.restTemplate = restTemplate;
    this.authRepository = authRepository;
    this.studentInheritanceRepository = studentInheritanceRepository;
    this.jwtTokenProvider = jwtTokenProvider;
    this.passwordEncoder = passwordEncoder;
    restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
  }

  @BeforeEach
  void clean() {
    authRepository.deleteAll();
    studentInheritanceRepository.deleteAll();
  }

  @Test
  void admin_lists_all_students_with_meta() {
    saveStudent(
        "2023-001",
        "mdupont",
        "Marie",
        "Dupont",
        Level.L2,
        LearningPath.EL,
        StudentStatus.ACTIVE,
        null);
    saveStudent(
        "2023-002",
        "jmoreau",
        "Julie",
        "Moreau",
        Level.L3,
        LearningPath.TN,
        StudentStatus.ACTIVE,
        "CQ1");

    ResponseEntity<StudentListResponse> response = getStudents("", adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    StudentListResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.meta().page()).isEqualTo(1);
    assertThat(body.meta().size()).isEqualTo(10);
    assertThat(body.meta().total()).isEqualTo(2);
    assertThat(body.data()).hasSize(2);
    assertThat(body.data())
        .extracting(UserExtendStudent::username)
        .containsExactlyInAnyOrder("mdupont", "jmoreau");
    assertThat(body.data())
        .extracting(student -> student.studentInheritance().ref())
        .containsExactlyInAnyOrder("2023-001", "2023-002");
  }

  @Test
  void teacher_can_list_students() {
    saveStudent(
        "2023-001",
        "mdupont",
        "Marie",
        "Dupont",
        Level.L2,
        LearningPath.EL,
        StudentStatus.ACTIVE,
        null);

    ResponseEntity<StudentListResponse> response = getStudents("", teacherToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().meta().total()).isEqualTo(1);
  }

  @Test
  void search_matches_ref_first_name_and_last_name_case_insensitive() {
    saveStudent(
        "2023-001",
        "mdupont",
        "Marie",
        "Dupont",
        Level.L2,
        LearningPath.EL,
        StudentStatus.ACTIVE,
        null);
    saveStudent(
        "2023-002",
        "jmoreau",
        "Julie",
        "Moreau",
        Level.L3,
        LearningPath.TN,
        StudentStatus.ACTIVE,
        null);
    String admin = adminToken();

    StudentListResponse byFirstName = getStudents("?search=marie", admin).getBody();
    assertThat(byFirstName).isNotNull();
    assertThat(byFirstName.data()).hasSize(1);
    assertThat(byFirstName.data().get(0).username()).isEqualTo("mdupont");

    StudentListResponse byLastName = getStudents("?search=MOREAU", admin).getBody();
    assertThat(byLastName).isNotNull();
    assertThat(byLastName.data()).hasSize(1);
    assertThat(byLastName.data().get(0).username()).isEqualTo("jmoreau");

    StudentListResponse byRef = getStudents("?search=2023-002", admin).getBody();
    assertThat(byRef).isNotNull();
    assertThat(byRef.data()).hasSize(1);
    assertThat(byRef.data().get(0).username()).isEqualTo("jmoreau");
  }

  @Test
  void learning_path_filters_exactly() {
    saveStudent(
        "2023-001",
        "mdupont",
        "Marie",
        "Dupont",
        Level.L2,
        LearningPath.EL,
        StudentStatus.ACTIVE,
        null);
    saveStudent(
        "2023-002",
        "jmoreau",
        "Julie",
        "Moreau",
        Level.L3,
        LearningPath.TN,
        StudentStatus.ACTIVE,
        null);
    saveStudent(
        "2023-003",
        "sbernard",
        "Sara",
        "Bernard",
        Level.L2,
        LearningPath.COMMON,
        StudentStatus.ACTIVE,
        null);

    StudentListResponse response = getStudents("?learningPath=EL", adminToken()).getBody();

    assertThat(response).isNotNull();
    assertThat(response.meta().total()).isEqualTo(1);
    assertThat(response.data()).hasSize(1);
    assertThat(response.data().get(0).username()).isEqualTo("mdupont");
  }

  @Test
  void student_status_filters_exactly() {
    saveStudent(
        "2023-001",
        "mdupont",
        "Marie",
        "Dupont",
        Level.L2,
        LearningPath.EL,
        StudentStatus.ACTIVE,
        null);
    saveStudent(
        "2023-002",
        "jmoreau",
        "Julie",
        "Moreau",
        Level.L3,
        LearningPath.TN,
        StudentStatus.INACTIVE,
        null);

    StudentListResponse response = getStudents("?studentStatus=INACTIVE", adminToken()).getBody();

    assertThat(response).isNotNull();
    assertThat(response.meta().total()).isEqualTo(1);
    assertThat(response.data()).hasSize(1);
    assertThat(response.data().get(0).username()).isEqualTo("jmoreau");
    assertThat(response.data().get(0).studentInheritance().studentStatus())
        .isEqualTo(StudentStatus.INACTIVE);
  }

  @Test
  void class_name_filters_exactly() {
    saveStudent(
        "2023-001",
        "mdupont",
        "Marie",
        "Dupont",
        Level.L2,
        LearningPath.EL,
        StudentStatus.ACTIVE,
        "CQ1");
    saveStudent(
        "2023-002",
        "jmoreau",
        "Julie",
        "Moreau",
        Level.L3,
        LearningPath.TN,
        StudentStatus.ACTIVE,
        "TSMA1");
    saveStudent(
        "2023-003",
        "sbernard",
        "Sara",
        "Bernard",
        Level.L2,
        LearningPath.EL,
        StudentStatus.ACTIVE,
        null);

    StudentListResponse response = getStudents("?className=CQ1", adminToken()).getBody();

    assertThat(response).isNotNull();
    assertThat(response.meta().total()).isEqualTo(1);
    assertThat(response.data()).hasSize(1);
    assertThat(response.data().get(0).username()).isEqualTo("mdupont");
  }

  @Test
  void filters_combine() {
    saveStudent(
        "2023-001",
        "mdupont",
        "Marie",
        "Dupont",
        Level.L2,
        LearningPath.EL,
        StudentStatus.ACTIVE,
        "CQ1");
    saveStudent(
        "2023-002",
        "jmoreau",
        "Julie",
        "Moreau",
        Level.L3,
        LearningPath.TN,
        StudentStatus.ACTIVE,
        "TSMA1");
    saveStudent(
        "2023-003",
        "mbernard",
        "Marie",
        "Bernard",
        Level.L2,
        LearningPath.EL,
        StudentStatus.INACTIVE,
        "TSMA1");

    StudentListResponse response =
        getStudents("?search=marie&learningPath=EL&className=CQ1", adminToken()).getBody();

    assertThat(response).isNotNull();
    assertThat(response.meta().total()).isEqualTo(1);
    assertThat(response.data()).hasSize(1);
    assertThat(response.data().get(0).username()).isEqualTo("mdupont");
  }

  @Test
  void pagination_is_honored() {
    saveStudent(
        "2023-001",
        "mdupont",
        "Marie",
        "Dupont",
        Level.L2,
        LearningPath.EL,
        StudentStatus.ACTIVE,
        null);
    saveStudent(
        "2023-002",
        "jmoreau",
        "Julie",
        "Moreau",
        Level.L3,
        LearningPath.TN,
        StudentStatus.ACTIVE,
        null);
    saveStudent(
        "2023-003",
        "sbernard",
        "Sara",
        "Bernard",
        Level.L2,
        LearningPath.EL,
        StudentStatus.ACTIVE,
        null);
    saveStudent(
        "2023-004",
        "ldupont",
        "Luc",
        "Dupont",
        Level.L3,
        LearningPath.TN,
        StudentStatus.ACTIVE,
        null);
    saveStudent(
        "2023-005",
        "amoreau",
        "Alice",
        "Moreau",
        Level.L2,
        LearningPath.COMMON,
        StudentStatus.ACTIVE,
        null);
    String admin = adminToken();

    StudentListResponse firstPage = getStudents("?page=1&size=2", admin).getBody();
    assertThat(firstPage).isNotNull();
    assertThat(firstPage.data()).hasSize(2);
    assertThat(firstPage.meta().page()).isEqualTo(1);
    assertThat(firstPage.meta().size()).isEqualTo(2);
    assertThat(firstPage.meta().total()).isEqualTo(5);

    StudentListResponse secondPage = getStudents("?page=2&size=2", admin).getBody();
    assertThat(secondPage).isNotNull();
    assertThat(secondPage.data()).hasSize(2);
    assertThat(secondPage.meta().page()).isEqualTo(2);
    assertThat(secondPage.meta().total()).isEqualTo(5);
    assertThat(firstPage.data().get(0).username())
        .isNotEqualTo(secondPage.data().get(0).username());

    StudentListResponse all = getStudents("?size=100", admin).getBody();
    assertThat(all).isNotNull();
    assertThat(all.data()).hasSize(5);
    assertThat(all.meta().total()).isEqualTo(5);
  }

  @Test
  void invalid_page_or_size_falls_back_to_defaults() {
    saveStudent(
        "2023-001",
        "mdupont",
        "Marie",
        "Dupont",
        Level.L2,
        LearningPath.EL,
        StudentStatus.ACTIVE,
        null);
    saveStudent(
        "2023-002",
        "jmoreau",
        "Julie",
        "Moreau",
        Level.L3,
        LearningPath.TN,
        StudentStatus.ACTIVE,
        null);
    saveStudent(
        "2023-003",
        "sbernard",
        "Sara",
        "Bernard",
        Level.L2,
        LearningPath.EL,
        StudentStatus.ACTIVE,
        null);
    saveStudent(
        "2023-004",
        "ldupont",
        "Luc",
        "Dupont",
        Level.L3,
        LearningPath.TN,
        StudentStatus.ACTIVE,
        null);
    saveStudent(
        "2023-005",
        "amoreau",
        "Alice",
        "Moreau",
        Level.L2,
        LearningPath.COMMON,
        StudentStatus.ACTIVE,
        null);
    String admin = adminToken();

    StudentListResponse negative = getStudents("?page=-3&size=-1", admin).getBody();
    assertThat(negative).isNotNull();
    assertThat(negative.meta().page()).isEqualTo(1);
    assertThat(negative.meta().size()).isEqualTo(10);
    assertThat(negative.data()).hasSize(5);

    StudentListResponse zero = getStudents("?page=0&size=0", admin).getBody();
    assertThat(zero).isNotNull();
    assertThat(zero.meta().page()).isEqualTo(1);
    assertThat(zero.meta().size()).isEqualTo(10);

    StudentListResponse tooLarge = getStudents("?page=101&size=101", admin).getBody();
    assertThat(tooLarge).isNotNull();
    assertThat(tooLarge.meta().page()).isEqualTo(1);
    assertThat(tooLarge.meta().size()).isEqualTo(10);
    assertThat(tooLarge.data()).hasSize(5);
  }

  @Test
  void non_student_users_are_excluded() {
    saveStudent(
        "2023-001",
        "mdupont",
        "Marie",
        "Dupont",
        Level.L2,
        LearningPath.EL,
        StudentStatus.ACTIVE,
        null);
    saveUser("p_martin", "Paul", "Martin", UserRole.TEACHER);

    StudentListResponse response = getStudents("", adminToken()).getBody();

    assertThat(response).isNotNull();
    assertThat(response.meta().total()).isEqualTo(1);
    assertThat(response.data().get(0).username()).isEqualTo("mdupont");
  }

  @Test
  void missing_token_is_unauthorized() {
    ResponseEntity<String> response = getStudentsError("", null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("Authentication required.");
  }

  @Test
  void student_role_is_forbidden() {
    ResponseEntity<String> response = getStudentsError("", studentToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Insufficient privileges.");
  }

  @Test
  void invalid_learning_path_is_bad_request() {
    ResponseEntity<String> response = getStudentsError("?learningPath=NOPE", adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("Invalid parameter: learningPath");
  }

  @Test
  void invalid_student_status_is_bad_request() {
    ResponseEntity<String> response = getStudentsError("?studentStatus=NOPE", adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("Invalid parameter: studentStatus");
  }

  @Test
  void invalid_search_chars_are_unprocessable() {
    ResponseEntity<String> response = getStudentsError("?search=marie!", adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("Search marie! is invalid");
  }

  @Test
  void invalid_class_name_chars_are_unprocessable() {
    ResponseEntity<String> response = getStudentsError("?className=CQ1!", adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("Search CQ1! is invalid");
  }

  private JUser saveStudent(
      String ref,
      String username,
      String firstName,
      String lastName,
      Level level,
      LearningPath learningPath,
      StudentStatus status,
      String className) {
    JStudentInheritance inheritance =
        studentInheritanceRepository.save(
            JStudentInheritance.builder()
                .ref(ref)
                .level(level)
                .learningPath(learningPath)
                .studentStatus(status)
                .className(className)
                .build());
    return authRepository.save(
        JUser.builder()
            .username(username)
            .password(passwordEncoder.encode("StrongPass12!"))
            .firstName(firstName)
            .lastName(lastName)
            .email(username + "@hacheuil.edu")
            .role(UserRole.STUDENT)
            .studentInheritance(inheritance)
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
    JUser teacher = saveUser("terry_teacher", "Terry", "Teacher", UserRole.TEACHER);
    return jwtTokenProvider.generateToken(teacher.getId().toString(), teacher.getRole().name());
  }

  private String studentToken() {
    JUser student = saveUser("sam_student", "Sam", "Student", UserRole.STUDENT);
    return jwtTokenProvider.generateToken(student.getId().toString(), student.getRole().name());
  }

  private ResponseEntity<StudentListResponse> getStudents(String query, String token) {
    return restTemplate.exchange(
        "/students" + query,
        HttpMethod.GET,
        new HttpEntity<>(jsonHeaders(token)),
        StudentListResponse.class);
  }

  private ResponseEntity<String> getStudentsError(String query, String token) {
    return restTemplate.exchange(
        "/students" + query, HttpMethod.GET, new HttpEntity<>(jsonHeaders(token)), String.class);
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
