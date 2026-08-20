package com.techindna.anerti.endpoint.rest.controller.reports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.dto.GradeReportInput;
import com.techindna.anerti.dto.GradeReportResponse;
import com.techindna.anerti.endpoint.event.EventProducer;
import com.techindna.anerti.endpoint.event.model.GradeReportRequested;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.ClassRepository;
import com.techindna.anerti.repository.CourseRepository;
import com.techindna.anerti.repository.ExamRepository;
import com.techindna.anerti.repository.GradeRepository;
import com.techindna.anerti.repository.StudentInheritanceRepository;
import com.techindna.anerti.repository.enums.LearningPath;
import com.techindna.anerti.repository.enums.Level;
import com.techindna.anerti.repository.enums.StudentStatus;
import com.techindna.anerti.repository.enums.UserRole;
import com.techindna.anerti.repository.model.JClass;
import com.techindna.anerti.repository.model.JCourse;
import com.techindna.anerti.repository.model.JExam;
import com.techindna.anerti.repository.model.JGrade;
import com.techindna.anerti.repository.model.JStudentInheritance;
import com.techindna.anerti.repository.model.JUser;
import com.techindna.anerti.security.jwt.JwtTokenProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
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
class PostGradeReportsIT extends FacadeIT {

  private final TestRestTemplate restTemplate;
  private final AuthRepository authRepository;
  private final StudentInheritanceRepository studentInheritanceRepository;
  private final CourseRepository courseRepository;
  private final ExamRepository examRepository;
  private final GradeRepository gradeRepository;
  private final ClassRepository classRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;

  @MockBean private EventProducer<GradeReportRequested> eventProducer;

  PostGradeReportsIT(
      TestRestTemplate restTemplate,
      AuthRepository authRepository,
      StudentInheritanceRepository studentInheritanceRepository,
      CourseRepository courseRepository,
      ExamRepository examRepository,
      GradeRepository gradeRepository,
      ClassRepository classRepository,
      JwtTokenProvider jwtTokenProvider,
      PasswordEncoder passwordEncoder) {
    this.restTemplate = restTemplate;
    this.authRepository = authRepository;
    this.studentInheritanceRepository = studentInheritanceRepository;
    this.courseRepository = courseRepository;
    this.examRepository = examRepository;
    this.gradeRepository = gradeRepository;
    this.classRepository = classRepository;
    this.jwtTokenProvider = jwtTokenProvider;
    this.passwordEncoder = passwordEncoder;
    restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
  }

  @BeforeEach
  void clean() {
    gradeRepository.deleteAll();
    examRepository.deleteAll();
    courseRepository.deleteAll();
    authRepository.deleteAll();
    studentInheritanceRepository.deleteAll();
    classRepository.deleteAll();
  }

  @Test
  void admin_generates_report_202() {
    JStudentInheritance student = saveStudent("S001");
    JUser admin = saveAdminUser();
    linkStudentToClass(student);

    JCourse course = saveCourse("EP1", "Digital Electronics", 6);
    JExam exam = saveExam(course.getId(), "1", "2024-2025", "2024-01-15T09:00:00Z");
    saveGrade(student.getId(), exam.getId(), "14", "Final");

    ResponseEntity<GradeReportResponse> response =
        postReport(new GradeReportInput(student.getId(), "2024-2025"), adminToken(admin));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    GradeReportResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.studentId()).isEqualTo(student.getId());
    assertThat(body.academicYear()).isEqualTo("2024-2025");
    assertThat(body.message()).contains("2024-2025");

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<GradeReportRequested>> captor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(captor.capture());
    GradeReportRequested event = captor.getValue().getFirst();
    assertThat(event.getStudentInheritanceId()).isEqualTo(student.getId());
    assertThat(event.getAcademicYear()).isEqualTo("2024-2025");
    assertThat(event.getRecipientEmail()).isEqualTo(admin.getEmail());
  }

  @Test
  void student_generates_own_report_202() {
    JStudentInheritance student = saveStudent("S001");
    JUser studentUser = saveStudentUser(student);
    linkStudentToClass(student);

    JCourse course = saveCourse("EP1", "Digital Electronics", 6);
    JExam exam = saveExam(course.getId(), "1", "2024-2025", "2024-01-15T09:00:00Z");
    saveGrade(student.getId(), exam.getId(), "14", "Final");

    ResponseEntity<GradeReportResponse> response =
        postReport(new GradeReportInput(student.getId(), "2024-2025"), studentToken(studentUser));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    GradeReportResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.studentId()).isEqualTo(student.getId());

    verify(eventProducer).accept(any());
  }

  @Test
  void student_generates_other_report_403() {
    JStudentInheritance student1 = saveStudent("S001");
    JStudentInheritance student2 = saveStudent("S002");
    JUser student1User = saveStudentUser(student1);

    linkStudentToClass(student1);

    JCourse course = saveCourse("EP1", "Digital Electronics", 6);
    JExam exam = saveExam(course.getId(), "1", "2024-2025", "2024-01-15T09:00:00Z");
    saveGrade(student2.getId(), exam.getId(), "14", "Final");

    ResponseEntity<String> response =
        postReportError(
            new GradeReportInput(student2.getId(), "2024-2025"), studentToken(student1User));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Students can only generate their own reports.");
    verify(eventProducer, never()).accept(any());
  }

  @Test
  void teacher_role_is_forbidden() {
    ResponseEntity<String> response =
        postReportError(new GradeReportInput(UUID.randomUUID(), "2024-2025"), teacherToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Insufficient privileges.");
  }

  @Test
  void missing_token_401() {
    ResponseEntity<String> response =
        postReportError(new GradeReportInput(UUID.randomUUID(), "2024-2025"), null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    verify(eventProducer, never()).accept(any());
  }

  @Test
  void unknown_student_404() {
    UUID unknownId = UUID.randomUUID();

    ResponseEntity<String> response =
        postReportError(new GradeReportInput(unknownId, "2024-2025"), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody())
        .contains("Student inheritance %s not found".formatted(unknownId));
    verify(eventProducer, never()).accept(any());
  }

  @Test
  void student_with_no_grades_in_year_409() {
    JStudentInheritance student = saveStudent("S001");
    JUser admin = saveAdminUser();
    linkStudentToClass(student);

    JCourse course = saveCourse("EP1", "Digital Electronics", 6);
    JExam exam = saveExam(course.getId(), "1", "2023-2024", "2023-01-15T09:00:00Z");
    saveGrade(student.getId(), exam.getId(), "14", "Final");

    ResponseEntity<String> response =
        postReportError(new GradeReportInput(student.getId(), "2024-2025"), adminToken(admin));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).contains("Student has no grades for academic year 2024-2025");
    verify(eventProducer, never()).accept(any());
  }

  @Test
  void student_with_grades_but_no_class_409() {
    JStudentInheritance student = saveStudent("S001");
    JUser admin = saveAdminUser();

    JCourse course = saveCourse("EP1", "Digital Electronics", 6);
    JExam exam = saveExam(course.getId(), "1", "2024-2025", "2024-01-15T09:00:00Z");
    saveGrade(student.getId(), exam.getId(), "14", "Final");

    ResponseEntity<String> response =
        postReportError(new GradeReportInput(student.getId(), "2024-2025"), adminToken(admin));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).contains("Student is not enrolled in any class.");
    verify(eventProducer, never()).accept(any());
  }

  @Test
  void null_student_inheritance_id_422() {
    ResponseEntity<String> response =
        postReportError(new GradeReportInput(null, "2024-2025"), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("studentInheritanceId is required and cannot be blank");
    verify(eventProducer, never()).accept(any());
  }

  @Test
  void invalid_academic_year_422() {
    JStudentInheritance student = saveStudent("S001");

    ResponseEntity<String> response =
        postReportError(new GradeReportInput(student.getId(), "2024-25"), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody())
        .contains("academicYear is invalid, the format must be YYYY-YYYY");
    verify(eventProducer, never()).accept(any());
  }

  @Test
  void malformed_body_400() {
    HttpHeaders headers = jsonHeaders(adminToken());

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/grade-reports",
            HttpMethod.POST,
            new HttpEntity<>("{not-json", headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("Request body is missing or malformed.");
    verify(eventProducer, never()).accept(any());
  }

  private JStudentInheritance saveStudent(String ref) {
    return studentInheritanceRepository.save(
        JStudentInheritance.builder()
            .ref(ref)
            .level(Level.L2)
            .learningPath(LearningPath.EL)
            .studentStatus(StudentStatus.ACTIVE)
            .build());
  }

  private void linkStudentToClass(JStudentInheritance student) {
    JClass cls = classRepository.save(JClass.builder().name("L2-EL-A").yearOf(2024).build());
    student.setClassId(cls.getId());
    studentInheritanceRepository.save(student);
  }

  private JUser saveAdminUser() {
    return authRepository.save(
        JUser.builder()
            .username("root_admin")
            .password(passwordEncoder.encode("StrongPass12!"))
            .firstName("Root")
            .lastName("Admin")
            .email("root.admin@hacheuil.edu")
            .role(UserRole.ADMIN)
            .build());
  }

  private JUser saveStudentUser(JStudentInheritance inheritance) {
    return authRepository.save(
        JUser.builder()
            .username("student_" + inheritance.getRef())
            .password(passwordEncoder.encode("StrongPass12!"))
            .firstName("Test")
            .lastName("Student")
            .email("student_" + inheritance.getRef() + "@hacheuil.edu")
            .role(UserRole.STUDENT)
            .studentInheritance(inheritance)
            .build());
  }

  private JCourse saveCourse(String ref, String title, int credits) {
    return courseRepository.save(JCourse.builder().ref(ref).title(title).credits(credits).build());
  }

  private JExam saveExam(UUID courseId, String coefficient, String academicYear, String date) {
    return examRepository.save(
        JExam.builder()
            .courseId(courseId)
            .coefficient(new BigDecimal(coefficient))
            .academicYear(academicYear)
            .date(Instant.parse(date))
            .build());
  }

  private JGrade saveGrade(
      UUID studentInheritanceId, UUID examId, String value, String description) {
    return gradeRepository.save(
        JGrade.builder()
            .studentInheritanceId(studentInheritanceId)
            .examId(examId)
            .value(new BigDecimal(value))
            .description(description)
            .build());
  }

  private String adminToken() {
    JUser admin = saveAdminUser();
    return adminToken(admin);
  }

  private String adminToken(JUser admin) {
    return jwtTokenProvider.generateToken(admin.getId().toString(), admin.getRole().name());
  }

  private String studentToken(JUser student) {
    return jwtTokenProvider.generateToken(student.getId().toString(), student.getRole().name());
  }

  private String teacherToken() {
    JUser teacher =
        authRepository.save(
            JUser.builder()
                .username("terry_teacher")
                .password(passwordEncoder.encode("StrongPass12!"))
                .firstName("Terry")
                .lastName("Teacher")
                .email("terry.teacher@hacheuil.edu")
                .role(UserRole.TEACHER)
                .build());
    return jwtTokenProvider.generateToken(teacher.getId().toString(), teacher.getRole().name());
  }

  private ResponseEntity<GradeReportResponse> postReport(GradeReportInput request, String token) {
    return restTemplate.exchange(
        "/grade-reports",
        HttpMethod.POST,
        new HttpEntity<>(request, jsonHeaders(token)),
        GradeReportResponse.class);
  }

  private ResponseEntity<String> postReportError(GradeReportInput request, String token) {
    return restTemplate.exchange(
        "/grade-reports",
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
