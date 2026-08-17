package com.techindna.anerti.endpoint.rest.controller.grades;

import static org.assertj.core.api.Assertions.assertThat;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.dto.CreateGradeInput;
import com.techindna.anerti.dto.GradeOutput;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.CourseRepository;
import com.techindna.anerti.repository.ExamRepository;
import com.techindna.anerti.repository.GradeRepository;
import com.techindna.anerti.repository.StudentInheritanceRepository;
import com.techindna.anerti.repository.TeacherCourseRepository;
import com.techindna.anerti.repository.TeacherInheritanceRepository;
import com.techindna.anerti.repository.enums.LearningPath;
import com.techindna.anerti.repository.enums.Level;
import com.techindna.anerti.repository.enums.StudentStatus;
import com.techindna.anerti.repository.enums.TeacherStatus;
import com.techindna.anerti.repository.enums.UserRole;
import com.techindna.anerti.repository.model.JCourse;
import com.techindna.anerti.repository.model.JExam;
import com.techindna.anerti.repository.model.JGrade;
import com.techindna.anerti.repository.model.JStudentInheritance;
import com.techindna.anerti.repository.model.JTeacherCourse;
import com.techindna.anerti.repository.model.JTeacherInheritance;
import com.techindna.anerti.repository.model.JUser;
import com.techindna.anerti.security.jwt.JwtTokenProvider;
import java.math.BigDecimal;
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
class PostGradesIT extends FacadeIT {

  private final TestRestTemplate restTemplate;
  private final AuthRepository authRepository;
  private final CourseRepository courseRepository;
  private final ExamRepository examRepository;
  private final GradeRepository gradeRepository;
  private final TeacherInheritanceRepository teacherInheritanceRepository;
  private final StudentInheritanceRepository studentInheritanceRepository;
  private final TeacherCourseRepository teacherCourseRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;

  PostGradesIT(
      TestRestTemplate restTemplate,
      AuthRepository authRepository,
      CourseRepository courseRepository,
      ExamRepository examRepository,
      GradeRepository gradeRepository,
      TeacherInheritanceRepository teacherInheritanceRepository,
      StudentInheritanceRepository studentInheritanceRepository,
      TeacherCourseRepository teacherCourseRepository,
      JwtTokenProvider jwtTokenProvider,
      PasswordEncoder passwordEncoder) {
    this.restTemplate = restTemplate;
    this.authRepository = authRepository;
    this.courseRepository = courseRepository;
    this.examRepository = examRepository;
    this.gradeRepository = gradeRepository;
    this.teacherInheritanceRepository = teacherInheritanceRepository;
    this.studentInheritanceRepository = studentInheritanceRepository;
    this.teacherCourseRepository = teacherCourseRepository;
    this.jwtTokenProvider = jwtTokenProvider;
    this.passwordEncoder = passwordEncoder;
    restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
  }

  @BeforeEach
  void clean() {
    gradeRepository.deleteAll();
    examRepository.deleteAll();
    teacherCourseRepository.deleteAll();
    courseRepository.deleteAll();
    authRepository.deleteAll();
    studentInheritanceRepository.deleteAll();
    teacherInheritanceRepository.deleteAll();
  }

  @Test
  void admin_creates_grade() {
    JCourse course = saveCourse();
    JExam exam = saveExam(course);
    JStudentInheritance student = saveStudent();

    ResponseEntity<GradeOutput> response =
        postGrade(validRequest(student.getId(), exam.getId(), "14.50", "Final exam"), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    GradeOutput body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.studentInheritanceId()).isEqualTo(student.getId());
    assertThat(body.examId()).isEqualTo(exam.getId());
    assertThat(body.value()).isEqualByComparingTo("14.50");
    assertThat(body.description()).isEqualTo("Final exam");
    assertThat(body.createdAt()).isNotNull();

    JGrade saved = gradeRepository.findAll().getFirst();
    assertThat(saved.getStudentInheritanceId()).isEqualTo(student.getId());
    assertThat(saved.getExamId()).isEqualTo(exam.getId());
    assertThat(saved.getValue()).isEqualByComparingTo("14.50");
  }

  @Test
  void teacher_creates_grade() {
    JCourse course = saveCourse();
    JExam exam = saveExam(course);
    JStudentInheritance student = saveStudent();

    ResponseEntity<GradeOutput> response =
        postGrade(
            validRequest(student.getId(), exam.getId(), "14.50", "Final exam"),
            teacherTokenWithCourse(course));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().examId()).isEqualTo(exam.getId());
  }

  @Test
  void teacher_without_assignment_is_forbidden() {
    JCourse course = saveCourse();
    JExam exam = saveExam(course);
    JStudentInheritance student = saveStudent();

    ResponseEntity<String> response =
        postGradeError(
            validRequest(student.getId(), exam.getId(), "14.50", "Final exam"),
            teacherTokenWithoutCourse());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Cannot create grade for course");
  }

  @Test
  void teacher_assigned_to_another_course_is_forbidden() {
    JCourse course = saveCourse();
    JExam exam = saveExam(course);
    JCourse otherCourse =
        courseRepository.save(JCourse.builder().ref("PRJ1").title("Project").credits(6).build());
    JStudentInheritance student = saveStudent();

    ResponseEntity<String> response =
        postGradeError(
            validRequest(student.getId(), exam.getId(), "14.50", "Final exam"),
            teacherTokenWithCourse(otherCourse));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Cannot create grade for course");
  }

  @Test
  void missing_token_is_unauthorized() {
    JCourse course = saveCourse();
    JExam exam = saveExam(course);
    JStudentInheritance student = saveStudent();

    ResponseEntity<String> response =
        postGradeError(validRequest(student.getId(), exam.getId(), "14.50", "Final exam"), null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("Authentication required.");
  }

  @Test
  void student_role_is_forbidden() {
    JCourse course = saveCourse();
    JExam exam = saveExam(course);
    JStudentInheritance student = saveStudent();

    ResponseEntity<String> response =
        postGradeError(
            validRequest(student.getId(), exam.getId(), "14.50", "Final exam"), studentToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Insufficient privileges.");
  }

  @Test
  void unknown_student_is_not_found() {
    JCourse course = saveCourse();
    JExam exam = saveExam(course);
    UUID unknownStudent = UUID.randomUUID();

    ResponseEntity<String> response =
        postGradeError(
            validRequest(unknownStudent, exam.getId(), "14.50", "Final exam"), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody())
        .contains("Student inheritance %s not found".formatted(unknownStudent));
  }

  @Test
  void unknown_exam_is_not_found() {
    JStudentInheritance student = saveStudent();
    UUID unknownExam = UUID.randomUUID();

    ResponseEntity<String> response =
        postGradeError(
            validRequest(student.getId(), unknownExam, "14.50", "Final exam"), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).contains("Exam %s not found".formatted(unknownExam));
  }

  @Test
  void multiple_grades_for_same_student_exam_are_allowed() {
    JCourse course = saveCourse();
    JExam exam = saveExam(course);
    JStudentInheritance student = saveStudent();
    gradeRepository.save(
        JGrade.builder()
            .studentInheritanceId(student.getId())
            .examId(exam.getId())
            .value(new BigDecimal("14.50"))
            .description("Original grade")
            .build());

    ResponseEntity<GradeOutput> response =
        postGrade(
            validRequest(student.getId(), exam.getId(), "15.00", "After claim"), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().value()).isEqualByComparingTo("15.00");
    assertThat(response.getBody().description()).isEqualTo("After claim");
    assertThat(gradeRepository.count()).isEqualTo(2);
  }

  @Test
  void null_student_inheritance_id_is_unprocessable() {
    JCourse course = saveCourse();
    JExam exam = saveExam(course);

    ResponseEntity<String> response =
        postGradeError(validRequest(null, exam.getId(), "14.50", "Final exam"), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("studentInheritanceId is required and cannot be blank");
  }

  @Test
  void null_exam_id_is_unprocessable() {
    JStudentInheritance student = saveStudent();

    ResponseEntity<String> response =
        postGradeError(validRequest(student.getId(), null, "14.50", "Final exam"), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("examId is required and cannot be blank");
  }

  @Test
  void null_value_is_bad_request() {
    JCourse course = saveCourse();
    JExam exam = saveExam(course);
    JStudentInheritance student = saveStudent();

    ResponseEntity<String> response =
        postGradeError(
            validRequest(student.getId(), exam.getId(), null, "Final exam"), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("value is required and cannot be blank");
  }

  @Test
  void negative_value_is_bad_request() {
    JCourse course = saveCourse();
    JExam exam = saveExam(course);
    JStudentInheritance student = saveStudent();

    ResponseEntity<String> response =
        postGradeError(
            validRequest(student.getId(), exam.getId(), "-1", "Final exam"), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("value must be greater than or equal to 0");
  }

  @Test
  void value_over_20_is_unprocessable() {
    JCourse course = saveCourse();
    JExam exam = saveExam(course);
    JStudentInheritance student = saveStudent();

    ResponseEntity<String> response =
        postGradeError(
            validRequest(student.getId(), exam.getId(), "21", "Final exam"), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("value must not exceed 20");
  }

  @Test
  void value_beyond_two_decimals_is_unprocessable() {
    JCourse course = saveCourse();
    JExam exam = saveExam(course);
    JStudentInheritance student = saveStudent();

    ResponseEntity<String> response =
        postGradeError(
            validRequest(student.getId(), exam.getId(), "14.555", "Final exam"), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("value must have at most 2 decimal places");
  }

  @Test
  void null_description_is_unprocessable() {
    JCourse course = saveCourse();
    JExam exam = saveExam(course);
    JStudentInheritance student = saveStudent();

    ResponseEntity<String> response =
        postGradeError(validRequest(student.getId(), exam.getId(), "14.50", null), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("description is required and cannot be blank");
  }

  @Test
  void blank_description_is_unprocessable() {
    JCourse course = saveCourse();
    JExam exam = saveExam(course);
    JStudentInheritance student = saveStudent();

    ResponseEntity<String> response =
        postGradeError(validRequest(student.getId(), exam.getId(), "14.50", "  "), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("description is required and cannot be blank");
  }

  @Test
  void malformed_body_is_bad_request() {
    HttpHeaders headers = jsonHeaders(adminToken());

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/grades", HttpMethod.POST, new HttpEntity<>("{not-json", headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("Request body is missing or malformed.");
  }

  @Test
  void invalid_uuid_in_body_is_bad_request() {
    HttpHeaders headers = jsonHeaders(adminToken());
    String body =
        "{\"studentInheritanceId\":\"not-a-uuid\",\"examId\":\""
            + UUID.randomUUID()
            + "\",\"value\":14.5,\"description\":\"Final\"}";

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/grades", HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("Request body is missing or malformed.");
  }

  @Test
  void zero_value_is_accepted() {
    JCourse course = saveCourse();
    JExam exam = saveExam(course);
    JStudentInheritance student = saveStudent();

    ResponseEntity<GradeOutput> response =
        postGrade(validRequest(student.getId(), exam.getId(), "0", "Zero grade"), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().value()).isEqualByComparingTo("0");
  }

  @Test
  void max_value_20_is_accepted() {
    JCourse course = saveCourse();
    JExam exam = saveExam(course);
    JStudentInheritance student = saveStudent();

    ResponseEntity<GradeOutput> response =
        postGrade(validRequest(student.getId(), exam.getId(), "20", "Perfect score"), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().value()).isEqualByComparingTo("20");
  }

  private JCourse saveCourse() {
    return courseRepository.save(
        JCourse.builder().ref("EP1").title("Digital Electronics").credits(6).build());
  }

  private JExam saveExam(JCourse course) {
    return examRepository.save(
        JExam.builder()
            .courseId(course.getId())
            .coefficient(new BigDecimal("0.5"))
            .academicYear("2024-2025")
            .date(Instant.parse("2024-01-15T09:00:00Z"))
            .build());
  }

  private JStudentInheritance saveStudent() {
    return studentInheritanceRepository.save(
        JStudentInheritance.builder()
            .ref("S001")
            .level(Level.L2)
            .learningPath(LearningPath.EL)
            .studentStatus(StudentStatus.ACTIVE)
            .build());
  }

  private CreateGradeInput validRequest(
      UUID studentInheritanceId, UUID examId, String value, String description) {
    return new CreateGradeInput(
        studentInheritanceId, examId, value == null ? null : new BigDecimal(value), description);
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

  private JUser saveTeacherUser(JTeacherInheritance inheritance) {
    return authRepository.save(
        JUser.builder()
            .username("terry_teacher")
            .password(passwordEncoder.encode("StrongPass12!"))
            .firstName("Some")
            .lastName("User")
            .email("terry.teacher@hacheuil.edu")
            .role(UserRole.TEACHER)
            .teacherInheritance(inheritance)
            .build());
  }

  private String teacherTokenWithCourse(JCourse course) {
    JTeacherInheritance inheritance =
        teacherInheritanceRepository.save(
            JTeacherInheritance.builder().ref("T001").teacherStatus(TeacherStatus.ACTIVE).build());
    teacherCourseRepository.save(
        JTeacherCourse.builder()
            .teacherInheritance(inheritance)
            .courseId(course.getId())
            .assignedAt(Instant.parse("2023-09-01T08:00:00Z"))
            .build());
    JUser teacher = saveTeacherUser(inheritance);
    return jwtTokenProvider.generateToken(teacher.getId().toString(), teacher.getRole().name());
  }

  private String teacherTokenWithoutCourse() {
    JTeacherInheritance inheritance =
        teacherInheritanceRepository.save(
            JTeacherInheritance.builder().ref("T001").teacherStatus(TeacherStatus.ACTIVE).build());
    JUser teacher = saveTeacherUser(inheritance);
    return jwtTokenProvider.generateToken(teacher.getId().toString(), teacher.getRole().name());
  }

  private String studentToken() {
    JUser student = saveUser("sam_student", "sam.student@hacheuil.edu", UserRole.STUDENT);
    return jwtTokenProvider.generateToken(student.getId().toString(), student.getRole().name());
  }

  private ResponseEntity<GradeOutput> postGrade(CreateGradeInput request, String token) {
    return restTemplate.exchange(
        "/grades",
        HttpMethod.POST,
        new HttpEntity<>(request, jsonHeaders(token)),
        GradeOutput.class);
  }

  private ResponseEntity<String> postGradeError(CreateGradeInput request, String token) {
    return restTemplate.exchange(
        "/grades", HttpMethod.POST, new HttpEntity<>(request, jsonHeaders(token)), String.class);
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
