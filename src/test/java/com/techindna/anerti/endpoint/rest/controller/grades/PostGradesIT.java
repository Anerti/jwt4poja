package com.techindna.anerti.endpoint.rest.controller.grades;

import static org.assertj.core.api.Assertions.assertThat;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.dto.CreateGradeInput;
import com.techindna.anerti.dto.GradeOutput;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.CourseRepository;
import com.techindna.anerti.repository.ExamRepository;
import com.techindna.anerti.repository.GradeHistoryRepository;
import com.techindna.anerti.repository.GradeRepository;
import com.techindna.anerti.repository.StudentInheritanceRepository;
import com.techindna.anerti.repository.TeacherCourseRepository;
import com.techindna.anerti.repository.TeacherInheritanceRepository;
import com.techindna.anerti.repository.enums.Level;
import com.techindna.anerti.repository.enums.TeacherStatus;
import com.techindna.anerti.repository.enums.UserRole;
import com.techindna.anerti.repository.model.JCourse;
import com.techindna.anerti.repository.model.JExam;
import com.techindna.anerti.repository.model.JGrade;
import com.techindna.anerti.repository.model.JGradeHistory;
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
  private final StudentInheritanceRepository studentInheritanceRepository;
  private final TeacherInheritanceRepository teacherInheritanceRepository;
  private final CourseRepository courseRepository;
  private final TeacherCourseRepository teacherCourseRepository;
  private final ExamRepository examRepository;
  private final GradeRepository gradeRepository;
  private final GradeHistoryRepository gradeHistoryRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;

  PostGradesIT(
      TestRestTemplate restTemplate,
      AuthRepository authRepository,
      StudentInheritanceRepository studentInheritanceRepository,
      TeacherInheritanceRepository teacherInheritanceRepository,
      CourseRepository courseRepository,
      TeacherCourseRepository teacherCourseRepository,
      ExamRepository examRepository,
      GradeRepository gradeRepository,
      GradeHistoryRepository gradeHistoryRepository,
      JwtTokenProvider jwtTokenProvider,
      PasswordEncoder passwordEncoder) {
    this.restTemplate = restTemplate;
    this.authRepository = authRepository;
    this.studentInheritanceRepository = studentInheritanceRepository;
    this.teacherInheritanceRepository = teacherInheritanceRepository;
    this.courseRepository = courseRepository;
    this.teacherCourseRepository = teacherCourseRepository;
    this.examRepository = examRepository;
    this.gradeRepository = gradeRepository;
    this.gradeHistoryRepository = gradeHistoryRepository;
    this.jwtTokenProvider = jwtTokenProvider;
    this.passwordEncoder = passwordEncoder;
    restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
  }

  @BeforeEach
  void clean() {
    gradeHistoryRepository.deleteAll();
    gradeRepository.deleteAll();
    examRepository.deleteAll();
    teacherCourseRepository.deleteAll();
    authRepository.deleteAll();
    courseRepository.deleteAll();
    teacherInheritanceRepository.deleteAll();
    studentInheritanceRepository.deleteAll();
  }

  @Test
  void admin_records_grade_with_initial_history() {
    JStudentInheritance student = saveStudent();
    JExam exam = saveExam(saveCourse("EP1"), "0.5", 2023, "2024-01-15T09:00:00Z");

    ResponseEntity<GradeOutput> response =
        postGrade(
            new CreateGradeInput(
                student.getId(), exam.getId(), new BigDecimal("14.5"), "Final exam"),
            adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    GradeOutput body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.studentId()).isEqualTo(student.getId());
    assertThat(body.examId()).isEqualTo(exam.getId());
    assertThat(body.grade()).isEqualByComparingTo(new BigDecimal("14.5"));
    assertThat(body.createdAt()).isNotNull();

    JGrade saved = gradeRepository.findAll().getFirst();
    assertThat(saved.getStudentInheritance().getId()).isEqualTo(student.getId());
    assertThat(saved.getExamId()).isEqualTo(exam.getId());
    JGradeHistory history = gradeHistoryRepository.findAll().getFirst();
    assertThat(history.getGrade()).isEqualByComparingTo(new BigDecimal("14.5"));
    assertThat(history.getDescription()).isEqualTo("Final exam");
  }

  @Test
  void teacher_can_grade_exam_of_assigned_course() {
    JTeacherInheritance teacher = saveTeacher();
    JCourse course = saveCourse("EP1");
    assign(teacher, course);
    JExam exam = saveExam(course, "0.5", 2023, "2024-01-15T09:00:00Z");
    JStudentInheritance student = saveStudent();

    ResponseEntity<GradeOutput> response =
        postGrade(
            new CreateGradeInput(student.getId(), exam.getId(), new BigDecimal("12"), null),
            teacherToken(teacher));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().grade()).isEqualByComparingTo(new BigDecimal("12"));
  }

  @Test
  void teacher_cannot_grade_exam_of_unassigned_course() {
    JTeacherInheritance teacher = saveTeacher();
    JCourse course = saveCourse("EP1");
    JExam exam = saveExam(course, "0.5", 2023, "2024-01-15T09:00:00Z");
    JStudentInheritance student = saveStudent();

    ResponseEntity<String> response =
        postGradeError(
            new CreateGradeInput(student.getId(), exam.getId(), new BigDecimal("12"), null),
            teacherToken(teacher));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody())
        .contains(
            "Teacher %s is not assigned to course %s".formatted(teacher.getId(), course.getId()));
  }

  @Test
  void missing_token_is_unauthorized() {
    ResponseEntity<String> response =
        postGradeError(
            new CreateGradeInput(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("12"), null),
            null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("Authentication required.");
  }

  @Test
  void student_role_is_forbidden() {
    JStudentInheritance student = saveStudent();
    JExam exam = saveExam(saveCourse("EP1"), "0.5", 2023, "2024-01-15T09:00:00Z");

    ResponseEntity<String> response =
        postGradeError(
            new CreateGradeInput(student.getId(), exam.getId(), new BigDecimal("12"), null),
            studentToken(student));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Insufficient privileges.");
  }

  @Test
  void duplicate_grade_is_conflict() {
    JStudentInheritance student = saveStudent();
    JExam exam = saveExam(saveCourse("EP1"), "0.5", 2023, "2024-01-15T09:00:00Z");
    gradeRepository.save(JGrade.builder().studentInheritance(student).examId(exam.getId()).build());

    ResponseEntity<String> response =
        postGradeError(
            new CreateGradeInput(student.getId(), exam.getId(), new BigDecimal("12"), null),
            adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody())
        .contains(
            "Student %s already has a grade for exam %s".formatted(student.getId(), exam.getId()));
  }

  @Test
  void grade_above_range_is_unprocessable() {
    JStudentInheritance student = saveStudent();
    JExam exam = saveExam(saveCourse("EP1"), "0.5", 2023, "2024-01-15T09:00:00Z");

    ResponseEntity<String> response =
        postGradeError(
            new CreateGradeInput(student.getId(), exam.getId(), new BigDecimal("20.5"), null),
            adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("grade must be between 0 and 20");
  }

  @Test
  void grade_below_range_is_unprocessable() {
    JStudentInheritance student = saveStudent();
    JExam exam = saveExam(saveCourse("EP1"), "0.5", 2023, "2024-01-15T09:00:00Z");

    ResponseEntity<String> response =
        postGradeError(
            new CreateGradeInput(student.getId(), exam.getId(), new BigDecimal("-1"), null),
            adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("grade must be between 0 and 20");
  }

  @Test
  void missing_grade_is_unprocessable() {
    JStudentInheritance student = saveStudent();
    JExam exam = saveExam(saveCourse("EP1"), "0.5", 2023, "2024-01-15T09:00:00Z");

    ResponseEntity<String> response =
        postGradeError(
            new CreateGradeInput(student.getId(), exam.getId(), null, null), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("grade is required and cannot be blank");
  }

  @Test
  void unknown_exam_is_not_found() {
    JStudentInheritance student = saveStudent();

    ResponseEntity<String> response =
        postGradeError(
            new CreateGradeInput(student.getId(), UUID.randomUUID(), new BigDecimal("12"), null),
            adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).contains("Exam %s not found");
  }

  @Test
  void unknown_student_is_not_found() {
    JExam exam = saveExam(saveCourse("EP1"), "0.5", 2023, "2024-01-15T09:00:00Z");
    UUID unknownStudent = UUID.randomUUID();

    ResponseEntity<String> response =
        postGradeError(
            new CreateGradeInput(unknownStudent, exam.getId(), new BigDecimal("12"), null),
            adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).contains("Student %s not found".formatted(unknownStudent));
  }

  private JStudentInheritance saveStudent() {
    return studentInheritanceRepository.save(
        JStudentInheritance.builder().ref("S001").level(Level.L1).build());
  }

  private JTeacherInheritance saveTeacher() {
    return teacherInheritanceRepository.save(
        JTeacherInheritance.builder().ref("T001").teacherStatus(TeacherStatus.ACTIVE).build());
  }

  private JCourse saveCourse(String ref) {
    return courseRepository.save(
        JCourse.builder().ref(ref).title("Digital Electronics").credits(6).build());
  }

  private JExam saveExam(JCourse course, String coefficient, int academicYear, String date) {
    return examRepository.save(
        JExam.builder()
            .course(course)
            .coefficient(new BigDecimal(coefficient))
            .academicYear(academicYear)
            .date(Instant.parse(date))
            .build());
  }

  private void assign(JTeacherInheritance teacher, JCourse course) {
    teacherCourseRepository.save(
        JTeacherCourse.builder()
            .teacherInheritance(teacher)
            .courseId(course.getId())
            .assignedAt(Instant.parse("2023-09-01T08:00:00Z"))
            .build());
  }

  private JUser saveUser(
      String username,
      String email,
      UserRole role,
      JTeacherInheritance teacherInheritance,
      JStudentInheritance studentInheritance) {
    return authRepository.save(
        JUser.builder()
            .username(username)
            .password(passwordEncoder.encode("StrongPass12!"))
            .firstName("Some")
            .lastName("User")
            .email(email)
            .role(role)
            .teacherInheritance(teacherInheritance)
            .studentInheritance(studentInheritance)
            .build());
  }

  private String adminToken() {
    JUser admin = saveUser("root_admin", "root.admin@hacheuil.edu", UserRole.ADMIN, null, null);
    return jwtTokenProvider.generateToken(admin.getId().toString(), admin.getRole().name());
  }

  private String teacherToken(JTeacherInheritance teacher) {
    JUser teacherUser =
        saveUser("terry_teacher", "terry.teacher@hacheuil.edu", UserRole.TEACHER, teacher, null);
    return jwtTokenProvider.generateToken(
        teacherUser.getId().toString(), teacherUser.getRole().name());
  }

  private String studentToken(JStudentInheritance student) {
    JUser studentUser =
        saveUser("sam_student", "sam.student@hacheuil.edu", UserRole.STUDENT, null, student);
    return jwtTokenProvider.generateToken(
        studentUser.getId().toString(), studentUser.getRole().name());
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
