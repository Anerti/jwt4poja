package com.techindna.anerti.endpoint.rest.controller.enrollments;

import static org.assertj.core.api.Assertions.assertThat;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.dto.CreateStudentGroupInput;
import com.techindna.anerti.dto.CreateStudentGroupListResponse;
import com.techindna.anerti.dto.CreateStudentGroupOutput;
import com.techindna.anerti.dto.CreateStudentGroupRequest;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.GroupRepository;
import com.techindna.anerti.repository.StudentGroupRepository;
import com.techindna.anerti.repository.StudentInheritanceRepository;
import com.techindna.anerti.repository.enums.LearningPath;
import com.techindna.anerti.repository.enums.Level;
import com.techindna.anerti.repository.enums.StudentStatus;
import com.techindna.anerti.repository.enums.UserRole;
import com.techindna.anerti.repository.model.JGroup;
import com.techindna.anerti.repository.model.JStudentGroup;
import com.techindna.anerti.repository.model.JStudentInheritance;
import com.techindna.anerti.repository.model.JUser;
import com.techindna.anerti.security.jwt.JwtTokenProvider;
import java.time.Instant;
import java.util.List;
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
class PostStudentGroupsIT extends FacadeIT {

  private final TestRestTemplate restTemplate;
  private final AuthRepository authRepository;
  private final StudentInheritanceRepository studentInheritanceRepository;
  private final GroupRepository groupRepository;
  private final StudentGroupRepository studentGroupRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;

  PostStudentGroupsIT(
      TestRestTemplate restTemplate,
      AuthRepository authRepository,
      StudentInheritanceRepository studentInheritanceRepository,
      GroupRepository groupRepository,
      StudentGroupRepository studentGroupRepository,
      JwtTokenProvider jwtTokenProvider,
      PasswordEncoder passwordEncoder) {
    this.restTemplate = restTemplate;
    this.authRepository = authRepository;
    this.studentInheritanceRepository = studentInheritanceRepository;
    this.groupRepository = groupRepository;
    this.studentGroupRepository = studentGroupRepository;
    this.jwtTokenProvider = jwtTokenProvider;
    this.passwordEncoder = passwordEncoder;
    restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
  }

  @BeforeEach
  void clean() {
    studentGroupRepository.deleteAll();
    authRepository.deleteAll();
    studentInheritanceRepository.deleteAll();
    groupRepository.deleteAll();
  }

  @Test
  void admin_bulk_enrolls_students_in_a_group() {
    UUID studentA = saveStudent("2023-001");
    UUID studentB = saveStudent("2023-002");
    UUID groupId = saveGroup("CQ1");
    Instant joinedAt = Instant.parse("2023-09-04T09:00:00Z");
    CreateStudentGroupRequest request =
        new CreateStudentGroupRequest(
            List.of(
                new CreateStudentGroupInput(studentA, groupId, joinedAt),
                new CreateStudentGroupInput(studentB, groupId, joinedAt)));

    ResponseEntity<CreateStudentGroupListResponse> response = enroll(request, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    CreateStudentGroupListResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.meta().total()).isEqualTo(2);
    assertThat(body.meta().page()).isEqualTo(1);
    assertThat(body.meta().size()).isEqualTo(2);
    assertThat(body.data()).hasSize(2);
    assertThat(body.data())
        .extracting(CreateStudentGroupOutput::studentId)
        .containsExactlyInAnyOrder(studentA, studentB);
    assertThat(body.data()).extracting(CreateStudentGroupOutput::groupId).allMatch(groupId::equals);
    assertThat(body.data())
        .extracting(CreateStudentGroupOutput::joinedAt)
        .allMatch(joinedAt::equals);
    assertThat(body.data())
        .extracting(CreateStudentGroupOutput::leftAt)
        .allMatch(java.util.Objects::isNull);

    List<JStudentGroup> saved = studentGroupRepository.findAll();
    assertThat(saved).hasSize(2);
    assertThat(saved)
        .extracting(JStudentGroup::getStudentInheritanceId)
        .containsExactlyInAnyOrder(studentA, studentB);
    assertThat(saved).extracting(JStudentGroup::getGroupId).allMatch(groupId::equals);
  }

  @Test
  void missing_token_is_unauthorized() {
    CreateStudentGroupRequest request =
        new CreateStudentGroupRequest(
            List.of(
                new CreateStudentGroupInput(UUID.randomUUID(), UUID.randomUUID(), Instant.now())));

    ResponseEntity<String> response = enrollError(request, null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("Authentication required.");
  }

  @Test
  void teacher_role_is_forbidden() {
    CreateStudentGroupRequest request =
        new CreateStudentGroupRequest(
            List.of(
                new CreateStudentGroupInput(UUID.randomUUID(), UUID.randomUUID(), Instant.now())));

    ResponseEntity<String> response = enrollError(request, teacherToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Insufficient privileges.");
  }

  @Test
  void student_role_is_forbidden() {
    CreateStudentGroupRequest request =
        new CreateStudentGroupRequest(
            List.of(
                new CreateStudentGroupInput(UUID.randomUUID(), UUID.randomUUID(), Instant.now())));

    ResponseEntity<String> response = enrollError(request, studentToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Insufficient privileges.");
  }

  @Test
  void unknown_student_is_not_found() {
    UUID studentId = UUID.randomUUID();
    UUID groupId = saveGroup("CQ1");
    CreateStudentGroupRequest request =
        new CreateStudentGroupRequest(
            List.of(
                new CreateStudentGroupInput(
                    studentId, groupId, Instant.parse("2023-09-04T09:00:00Z"))));

    ResponseEntity<String> response = enrollError(request, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody())
        .contains("Student %s or group %s not found".formatted(studentId, groupId));
  }

  @Test
  void unknown_group_is_not_found() {
    UUID studentId = saveStudent("2023-001");
    UUID groupId = UUID.randomUUID();
    CreateStudentGroupRequest request =
        new CreateStudentGroupRequest(
            List.of(
                new CreateStudentGroupInput(
                    studentId, groupId, Instant.parse("2023-09-04T09:00:00Z"))));

    ResponseEntity<String> response = enrollError(request, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody())
        .contains("Student %s or group %s not found".formatted(studentId, groupId));
  }

  @Test
  void duplicate_pair_is_conflict() {
    UUID studentId = saveStudent("2023-001");
    UUID groupId = saveGroup("CQ1");
    Instant joinedAt = Instant.parse("2023-09-04T09:00:00Z");
    studentGroupRepository.save(
        JStudentGroup.builder()
            .studentInheritanceId(studentId)
            .groupId(groupId)
            .joinedAt(joinedAt)
            .build());
    CreateStudentGroupRequest request =
        new CreateStudentGroupRequest(
            List.of(new CreateStudentGroupInput(studentId, groupId, joinedAt)));

    ResponseEntity<String> response = enrollError(request, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).contains("already enrolled");
  }

  @Test
  void duplicate_pair_in_same_request_is_conflict() {
    UUID studentId = saveStudent("2023-001");
    UUID groupId = saveGroup("CQ1");
    Instant joinedAt = Instant.parse("2023-09-04T09:00:00Z");
    CreateStudentGroupRequest request =
        new CreateStudentGroupRequest(
            List.of(
                new CreateStudentGroupInput(studentId, groupId, joinedAt),
                new CreateStudentGroupInput(studentId, groupId, joinedAt)));

    ResponseEntity<String> response = enrollError(request, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).contains("already enrolled");
    assertThat(studentGroupRepository.count()).isZero();
  }

  @Test
  void null_joined_at_is_bad_request() {
    UUID studentId = saveStudent("2023-001");
    UUID groupId = saveGroup("CQ1");
    CreateStudentGroupRequest request =
        new CreateStudentGroupRequest(
            List.of(new CreateStudentGroupInput(studentId, groupId, null)));

    ResponseEntity<String> response = enrollError(request, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("joinedAt is required and cannot be blank");
  }

  @Test
  void null_student_id_is_bad_request() {
    UUID groupId = saveGroup("CQ1");
    CreateStudentGroupRequest request =
        new CreateStudentGroupRequest(
            List.of(
                new CreateStudentGroupInput(null, groupId, Instant.parse("2023-09-04T09:00:00Z"))));

    ResponseEntity<String> response = enrollError(request, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("studentId is required and cannot be blank");
  }

  @Test
  void malformed_body_is_bad_request() {
    HttpHeaders headers = jsonHeaders(adminToken());

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/student-groups",
            HttpMethod.POST,
            new HttpEntity<>("{not-json", headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("Request body is missing or malformed.");
  }

  private UUID saveStudent(String ref) {
    JStudentInheritance inheritance =
        studentInheritanceRepository.save(
            JStudentInheritance.builder()
                .ref(ref)
                .level(Level.L2)
                .learningPath(LearningPath.EL)
                .studentStatus(StudentStatus.ACTIVE)
                .build());
    authRepository.save(
        JUser.builder()
            .username("student_" + ref)
            .password(passwordEncoder.encode("StrongPass12!"))
            .firstName("Marie")
            .lastName("Dupont")
            .email(ref + "@hacheuil.edu")
            .role(UserRole.STUDENT)
            .studentInheritance(inheritance)
            .build());
    return inheritance.getId();
  }

  private UUID saveGroup(String ref) {
    return groupRepository.save(JGroup.builder().ref(ref).build()).getId();
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

  private ResponseEntity<CreateStudentGroupListResponse> enroll(
      CreateStudentGroupRequest request, String token) {
    return restTemplate.exchange(
        "/student-groups",
        HttpMethod.POST,
        new HttpEntity<>(request, jsonHeaders(token)),
        CreateStudentGroupListResponse.class);
  }

  private ResponseEntity<String> enrollError(CreateStudentGroupRequest request, String token) {
    return restTemplate.exchange(
        "/student-groups",
        HttpMethod.POST,
        new HttpEntity<>(request, jsonHeaders(token)),
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
