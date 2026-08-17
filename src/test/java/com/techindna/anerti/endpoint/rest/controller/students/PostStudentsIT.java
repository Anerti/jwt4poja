package com.techindna.anerti.endpoint.rest.controller.students;

import static org.assertj.core.api.Assertions.assertThat;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.dto.CreateStudentInput;
import com.techindna.anerti.dto.UserExtendStudent;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.ClassRepository;
import com.techindna.anerti.repository.GradeRepository;
import com.techindna.anerti.repository.GroupRepository;
import com.techindna.anerti.repository.StudentInheritanceRepository;
import com.techindna.anerti.repository.enums.LearningPath;
import com.techindna.anerti.repository.enums.Level;
import com.techindna.anerti.repository.enums.StudentStatus;
import com.techindna.anerti.repository.enums.UserRole;
import com.techindna.anerti.repository.model.JClass;
import com.techindna.anerti.repository.model.JGroup;
import com.techindna.anerti.repository.model.JStudentInheritance;
import com.techindna.anerti.repository.model.JUser;
import com.techindna.anerti.security.jwt.JwtTokenProvider;
import java.time.Instant;
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
class PostStudentsIT extends FacadeIT {

  private final TestRestTemplate restTemplate;
  private final AuthRepository authRepository;
  private final StudentInheritanceRepository studentInheritanceRepository;
  private final GradeRepository gradeRepository;
  private final ClassRepository classRepository;
  private final GroupRepository groupRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;

  PostStudentsIT(
      TestRestTemplate restTemplate,
      AuthRepository authRepository,
      StudentInheritanceRepository studentInheritanceRepository,
      GradeRepository gradeRepository,
      ClassRepository classRepository,
      GroupRepository groupRepository,
      JwtTokenProvider jwtTokenProvider,
      PasswordEncoder passwordEncoder) {
    this.restTemplate = restTemplate;
    this.authRepository = authRepository;
    this.studentInheritanceRepository = studentInheritanceRepository;
    this.gradeRepository = gradeRepository;
    this.classRepository = classRepository;
    this.groupRepository = groupRepository;
    this.jwtTokenProvider = jwtTokenProvider;
    this.passwordEncoder = passwordEncoder;
    restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
  }

  @BeforeEach
  void clean() {
    gradeRepository.deleteAll();
    authRepository.deleteAll();
    studentInheritanceRepository.deleteAll();
    classRepository.deleteAll();
    groupRepository.deleteAll();
  }

  @Test
  void admin_creates_student_with_defaults() {
    ResponseEntity<UserExtendStudent> response = postStudent(validInput(), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    UserExtendStudent body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.username()).isEqualTo("mdupont");
    assertThat(body.firstName()).isEqualTo("Marie");
    assertThat(body.lastName()).isEqualTo("Dupont");
    assertThat(body.email()).isEqualTo("marie.dupont@hacheuil.edu");
    assertThat(body.role()).isEqualTo(UserRole.STUDENT);
    assertThat(body.studentInheritance().ref()).isEqualTo("2023-001");
    assertThat(body.studentInheritance().level()).isEqualTo(Level.L2);
    assertThat(body.studentInheritance().learningPath()).isEqualTo(LearningPath.EL);
    assertThat(body.studentInheritance().studentStatus()).isEqualTo(StudentStatus.ACTIVE);
    assertThat(body.studentInheritance().joinedAt()).isNull();
    assertThat(body.groupId()).isNull();
    assertThat(body.classId()).isNull();

    JUser saved = authRepository.findByEmail("marie.dupont@hacheuil.edu").orElseThrow();
    assertThat(saved.getRole()).isEqualTo(UserRole.STUDENT);
    assertThat(saved.getStudentInheritance().getRef()).isEqualTo("2023-001");
    assertThat(saved.getStudentInheritance().getLevel()).isEqualTo(Level.L2);
    assertThat(saved.getStudentInheritance().getLearningPath()).isEqualTo(LearningPath.EL);
    assertThat(saved.getStudentInheritance().getStudentStatus()).isEqualTo(StudentStatus.ACTIVE);
    assertThat(saved.getStudentInheritance().getGroupId()).isNull();
    assertThat(saved.getStudentInheritance().getClassId()).isNull();
    assertThat(passwordEncoder.matches("StrongPass12!", saved.getPassword())).isTrue();
  }

  @Test
  void admin_creates_student_with_joined_at_group_and_class() {
    Instant joinedAt = Instant.parse("2023-09-04T09:00:00Z");
    JGroup group = groupRepository.save(JGroup.builder().ref("CQ1").build());
    JClass jclass = classRepository.save(JClass.builder().name("CQ1").yearOf(2023).build());
    CreateStudentInput input =
        withGroupId(
            withClassId(withJoinedAt(validInput(), joinedAt), jclass.getId()), group.getId());

    ResponseEntity<UserExtendStudent> response = postStudent(input, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    UserExtendStudent body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.studentInheritance().ref()).isEqualTo("2023-001");
    assertThat(body.studentInheritance().level()).isEqualTo(Level.L2);
    assertThat(body.studentInheritance().learningPath()).isEqualTo(LearningPath.EL);
    assertThat(body.studentInheritance().joinedAt()).isEqualTo(joinedAt);
    assertThat(body.groupId()).isEqualTo(group.getId());
    assertThat(body.classId()).isEqualTo(jclass.getId());

    JUser saved = authRepository.findByEmail("marie.dupont@hacheuil.edu").orElseThrow();
    assertThat(saved.getStudentInheritance().getGroupId()).isEqualTo(group.getId());
    assertThat(saved.getStudentInheritance().getClassId()).isEqualTo(jclass.getId());
  }

  @Test
  void missing_token_is_unauthorized() {
    ResponseEntity<String> response = postStudentError(validInput(), null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("Authentication required.");
  }

  @Test
  void teacher_role_is_forbidden() {
    ResponseEntity<String> response = postStudentError(validInput(), teacherToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Insufficient privileges.");
  }

  @Test
  void student_role_is_forbidden() {
    ResponseEntity<String> response = postStudentError(validInput(), studentToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Insufficient privileges.");
  }

  @Test
  void duplicate_username_is_conflict() {
    saveUser("mdupont", "other@hacheuil.edu", UserRole.STUDENT);

    ResponseEntity<String> response = postStudentError(validInput(), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).contains("Cannot use username mdupont");
  }

  @Test
  void duplicate_email_is_conflict() {
    saveUser("other_user", "marie.dupont@hacheuil.edu", UserRole.STUDENT);

    ResponseEntity<String> response = postStudentError(validInput(), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).contains("cannot use email marie.dupont@hacheuil.edu");
  }

  @Test
  void duplicate_ref_is_conflict() {
    studentInheritanceRepository.save(
        JStudentInheritance.builder()
            .ref("2023-001")
            .level(Level.L2)
            .learningPath(LearningPath.EL)
            .build());

    ResponseEntity<String> response = postStudentError(validInput(), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).contains("Cannot use ref 2023-001");
  }

  @Test
  void unknown_group_id_is_not_found() {
    UUID unknown = UUID.randomUUID();
    CreateStudentInput input = withGroupId(validInput(), unknown);

    ResponseEntity<String> response = postStudentError(input, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).contains("Group %s not found".formatted(unknown));
  }

  @Test
  void unknown_class_id_fails() {
    UUID unknown = UUID.randomUUID();
    CreateStudentInput input = withClassId(validInput(), unknown);

    ResponseEntity<String> response = postStudentError(input, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).contains("Something went wrong, please try again later");
  }

  @Test
  void weak_password_is_unprocessable() {
    CreateStudentInput input = withPassword(validInput(), "short");

    ResponseEntity<String> response = postStudentError(input, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("Password must be at least 12 characters");
  }

  @Test
  void invalid_email_is_unprocessable() {
    CreateStudentInput input = withEmail(validInput(), "not-an-email");

    ResponseEntity<String> response = postStudentError(input, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("Email not-an-email is not valid");
  }

  @Test
  void blank_first_name_is_unprocessable() {
    CreateStudentInput input = withFirstName(validInput(), "");

    ResponseEntity<String> response = postStudentError(input, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("firstName is required and cannot be blank");
  }

  @Test
  void invalid_ref_is_unprocessable() {
    CreateStudentInput input = withRef(validInput(), "2023 001");

    ResponseEntity<String> response = postStudentError(input, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("Ref 2023 001 is invalid");
  }

  @Test
  void missing_level_is_unprocessable() {
    CreateStudentInput input = withLevel(validInput(), null);

    ResponseEntity<String> response = postStudentError(input, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("level is required and cannot be blank");
  }

  @Test
  void missing_learning_path_is_unprocessable() {
    CreateStudentInput input = withLearningPath(validInput(), null);

    ResponseEntity<String> response = postStudentError(input, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("learningPath is required and cannot be blank");
  }

  @Test
  void invalid_enum_value_is_bad_request() {
    HttpHeaders headers = jsonHeaders(adminToken());
    String body =
        """
        {
          "username": "mdupont",
          "password": "StrongPass12!",
          "firstName": "Marie",
          "lastName": "Dupont",
          "email": "marie.dupont@hacheuil.edu",
          "ref": "2023-001",
          "level": "NOPE",
          "learningPath": "EL"
        }
        """;

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/students", HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("Request body is missing or malformed.");
  }

  @Test
  void malformed_body_is_bad_request() {
    HttpHeaders headers = jsonHeaders(adminToken());

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/students", HttpMethod.POST, new HttpEntity<>("{not-json", headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("Request body is missing or malformed.");
  }

  private CreateStudentInput validInput() {
    return new CreateStudentInput(
        "mdupont",
        "StrongPass12!",
        "Marie",
        "Dupont",
        "marie.dupont@hacheuil.edu",
        "2023-001",
        null,
        Level.L2,
        LearningPath.EL,
        null,
        null);
  }

  private CreateStudentInput withPassword(CreateStudentInput request, String password) {
    return new CreateStudentInput(
        request.username(),
        password,
        request.firstName(),
        request.lastName(),
        request.email(),
        request.ref(),
        request.joinedAt(),
        request.level(),
        request.learningPath(),
        request.groupId(),
        request.classId());
  }

  private CreateStudentInput withEmail(CreateStudentInput request, String email) {
    return new CreateStudentInput(
        request.username(),
        request.password(),
        request.firstName(),
        request.lastName(),
        email,
        request.ref(),
        request.joinedAt(),
        request.level(),
        request.learningPath(),
        request.groupId(),
        request.classId());
  }

  private CreateStudentInput withFirstName(CreateStudentInput request, String firstName) {
    return new CreateStudentInput(
        request.username(),
        request.password(),
        firstName,
        request.lastName(),
        request.email(),
        request.ref(),
        request.joinedAt(),
        request.level(),
        request.learningPath(),
        request.groupId(),
        request.classId());
  }

  private CreateStudentInput withRef(CreateStudentInput request, String ref) {
    return new CreateStudentInput(
        request.username(),
        request.password(),
        request.firstName(),
        request.lastName(),
        request.email(),
        ref,
        request.joinedAt(),
        request.level(),
        request.learningPath(),
        request.groupId(),
        request.classId());
  }

  private CreateStudentInput withLevel(CreateStudentInput request, Level level) {
    return new CreateStudentInput(
        request.username(),
        request.password(),
        request.firstName(),
        request.lastName(),
        request.email(),
        request.ref(),
        request.joinedAt(),
        level,
        request.learningPath(),
        request.groupId(),
        request.classId());
  }

  private CreateStudentInput withLearningPath(
      CreateStudentInput request, LearningPath learningPath) {
    return new CreateStudentInput(
        request.username(),
        request.password(),
        request.firstName(),
        request.lastName(),
        request.email(),
        request.ref(),
        request.joinedAt(),
        request.level(),
        learningPath,
        request.groupId(),
        request.classId());
  }

  private CreateStudentInput withJoinedAt(CreateStudentInput request, Instant joinedAt) {
    return new CreateStudentInput(
        request.username(),
        request.password(),
        request.firstName(),
        request.lastName(),
        request.email(),
        request.ref(),
        joinedAt,
        request.level(),
        request.learningPath(),
        request.groupId(),
        request.classId());
  }

  private CreateStudentInput withGroupId(CreateStudentInput request, UUID groupId) {
    return new CreateStudentInput(
        request.username(),
        request.password(),
        request.firstName(),
        request.lastName(),
        request.email(),
        request.ref(),
        request.joinedAt(),
        request.level(),
        request.learningPath(),
        groupId,
        request.classId());
  }

  private CreateStudentInput withClassId(CreateStudentInput request, UUID classId) {
    return new CreateStudentInput(
        request.username(),
        request.password(),
        request.firstName(),
        request.lastName(),
        request.email(),
        request.ref(),
        request.joinedAt(),
        request.level(),
        request.learningPath(),
        request.groupId(),
        classId);
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

  private ResponseEntity<UserExtendStudent> postStudent(CreateStudentInput request, String token) {
    return restTemplate.exchange(
        "/students",
        HttpMethod.POST,
        new HttpEntity<>(request, jsonHeaders(token)),
        UserExtendStudent.class);
  }

  private ResponseEntity<String> postStudentError(CreateStudentInput request, String token) {
    return restTemplate.exchange(
        "/students", HttpMethod.POST, new HttpEntity<>(request, jsonHeaders(token)), String.class);
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
