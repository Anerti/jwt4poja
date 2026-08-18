package com.techindna.anerti.endpoint.rest.controller.reports;

import static org.assertj.core.api.Assertions.assertThat;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.dto.StudentGeneralAverage;
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
class GetStudentAverageIT extends FacadeIT {

  private final TestRestTemplate restTemplate;
  private final AuthRepository authRepository;
  private final StudentInheritanceRepository studentInheritanceRepository;
  private final CourseRepository courseRepository;
  private final ExamRepository examRepository;
  private final GradeRepository gradeRepository;
  private final TeacherInheritanceRepository teacherInheritanceRepository;
  private final TeacherCourseRepository teacherCourseRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;

  GetStudentAverageIT(
      TestRestTemplate restTemplate,
      AuthRepository authRepository,
      StudentInheritanceRepository studentInheritanceRepository,
      CourseRepository courseRepository,
      ExamRepository examRepository,
      GradeRepository gradeRepository,
      TeacherInheritanceRepository teacherInheritanceRepository,
      TeacherCourseRepository teacherCourseRepository,
      JwtTokenProvider jwtTokenProvider,
      PasswordEncoder passwordEncoder) {
    this.restTemplate = restTemplate;
    this.authRepository = authRepository;
    this.studentInheritanceRepository = studentInheritanceRepository;
    this.courseRepository = courseRepository;
    this.examRepository = examRepository;
    this.gradeRepository = gradeRepository;
    this.teacherInheritanceRepository = teacherInheritanceRepository;
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
  void admin_computes_average_no_grades_returns_zero() {
    JStudentInheritance student = saveStudent("S001");
    saveStudentUser(student);

    ResponseEntity<StudentGeneralAverage> response =
        getAverage(student.getId(), null, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    StudentGeneralAverage body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.studentInheritanceId()).isEqualTo(student.getId());
    assertThat(body.generalAverage()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void admin_computes_average_one_course() {
    JStudentInheritance student = saveStudent("S001");
    saveStudentUser(student);
    JCourse course = saveCourse("EP1", "Digital Electronics", 6);
    JExam exam = saveExam(course.getId(), "1", "2024-2025", "2024-01-15T09:00:00Z");
    saveGrade(student.getId(), exam.getId(), "14", "Final");

    ResponseEntity<StudentGeneralAverage> response =
        getAverage(student.getId(), null, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    StudentGeneralAverage body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.generalAverage()).isEqualByComparingTo("14");
  }

  @Test
  void admin_computes_average_two_courses() {
    JStudentInheritance student = saveStudent("S001");
    saveStudentUser(student);

    JCourse course1 = saveCourse("EP1", "Digital Electronics", 6);
    JExam exam1 = saveExam(course1.getId(), "1", "2024-2025", "2024-01-15T09:00:00Z");
    saveGrade(student.getId(), exam1.getId(), "16", "Exam 1");

    JCourse course2 = saveCourse("PRJ1", "Project", 4);
    JExam exam2 = saveExam(course2.getId(), "1", "2024-2025", "2024-03-15T09:00:00Z");
    saveGrade(student.getId(), exam2.getId(), "12", "Project");

    ResponseEntity<StudentGeneralAverage> response =
        getAverage(student.getId(), null, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    StudentGeneralAverage body = response.getBody();
    assertThat(body).isNotNull();
    // course1: 16 * 1 = 16, course2: 12 * 1 = 12
    // general: (16 * 6 + 12 * 4) / (6 + 4) = (96 + 48) / 10 = 14.4
    assertThat(body.generalAverage()).isEqualByComparingTo("14.4");
  }

  @Test
  void admin_computes_average_with_academic_year_filter() {
    JStudentInheritance student = saveStudent("S001");
    saveStudentUser(student);
    JCourse course = saveCourse("EP1", "Digital Electronics", 6);

    JExam exam2024 = saveExam(course.getId(), "1", "2024-2025", "2024-01-15T09:00:00Z");
    saveGrade(student.getId(), exam2024.getId(), "16", "2024 exam");

    JExam exam2023 = saveExam(course.getId(), "1", "2023-2024", "2023-01-15T09:00:00Z");
    saveGrade(student.getId(), exam2023.getId(), "10", "2023 exam");

    ResponseEntity<StudentGeneralAverage> response =
        getAverage(student.getId(), "2024-2025", adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    StudentGeneralAverage body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.academicYear()).isEqualTo("2024-2025");
    assertThat(body.generalAverage()).isEqualByComparingTo("16");
  }

  @Test
  void student_computes_own_average() {
    JStudentInheritance student = saveStudent("S001");
    JUser studentUser = saveStudentUser(student);
    JCourse course = saveCourse("EP1", "Digital Electronics", 6);
    JExam exam = saveExam(course.getId(), "1", "2024-2025", "2024-01-15T09:00:00Z");
    saveGrade(student.getId(), exam.getId(), "14", "Final");

    String token =
        jwtTokenProvider.generateToken(
            studentUser.getId().toString(), studentUser.getRole().name());

    ResponseEntity<StudentGeneralAverage> response = getAverage(student.getId(), null, token);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().generalAverage()).isEqualByComparingTo("14");
  }

  @Test
  void student_cannot_compute_other_student_average() {
    JStudentInheritance student1 = saveStudent("S001");
    JStudentInheritance student2 = saveStudent("S002");
    JUser student1User = saveStudentUser(student1);
    saveStudentUser(student2);

    String token =
        jwtTokenProvider.generateToken(
            student1User.getId().toString(), student1User.getRole().name());

    ResponseEntity<String> response = getAverageError(student2.getId(), null, token);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Students can only compute their own average.");
  }

  @Test
  void teacher_computes_average_for_assigned_course() {
    JStudentInheritance student = saveStudent("S001");
    saveStudentUser(student);
    JCourse course = saveCourse("EP1", "Digital Electronics", 6);
    JExam exam = saveExam(course.getId(), "1", "2024-2025", "2024-01-15T09:00:00Z");
    saveGrade(student.getId(), exam.getId(), "14", "Final");

    String token = teacherTokenWithCourse(course);

    ResponseEntity<StudentGeneralAverage> response = getAverage(student.getId(), null, token);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().generalAverage()).isEqualByComparingTo("14");
  }

  @Test
  void teacher_cannot_compute_average_for_unassigned_course() {
    JStudentInheritance student = saveStudent("S001");
    saveStudentUser(student);
    JCourse course = saveCourse("EP1", "Digital Electronics", 6);
    JExam exam = saveExam(course.getId(), "1", "2024-2025", "2024-01-15T09:00:00Z");
    saveGrade(student.getId(), exam.getId(), "14", "Final");

    JCourse otherCourse = saveCourse("PRJ1", "Project", 4);
    String token = teacherTokenWithCourse(otherCourse);

    ResponseEntity<StudentGeneralAverage> response = getAverage(student.getId(), null, token);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().generalAverage()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void unknown_student_inheritance_returns_not_found() {
    UUID unknownId = UUID.randomUUID();

    ResponseEntity<String> response = getAverageError(unknownId, null, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody())
        .contains("Student inheritance %s not found".formatted(unknownId));
  }

  @Test
  void missing_token_returns_unauthorized() {
    JStudentInheritance student = saveStudent("S001");

    ResponseEntity<String> response = getAverageError(student.getId(), null, null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("Authentication required.");
  }

  @Test
  void invalid_academic_year_returns_unprocessable() {
    JStudentInheritance student = saveStudent("S001");

    ResponseEntity<String> response = getAverageError(student.getId(), "2024-25", adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody())
        .contains("academicYear is invalid, the format must be YYYY-YYYY");
  }

  @Test
  void latest_grade_per_exam_is_used() {
    JStudentInheritance student = saveStudent("S001");
    saveStudentUser(student);
    JCourse course = saveCourse("EP1", "Digital Electronics", 6);
    JExam exam = saveExam(course.getId(), "1", "2024-2025", "2024-01-15T09:00:00Z");
    saveGrade(student.getId(), exam.getId(), "10", "First attempt");
    saveGrade(student.getId(), exam.getId(), "16", "Second attempt");

    ResponseEntity<StudentGeneralAverage> response =
        getAverage(student.getId(), null, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    StudentGeneralAverage body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.generalAverage()).isEqualByComparingTo("16");
  }

  @Test
  void weighted_average_with_two_exams() {
    JStudentInheritance student = saveStudent("S001");
    saveStudentUser(student);
    JCourse course = saveCourse("EP1", "Digital Electronics", 6);
    JExam exam1 = saveExam(course.getId(), "0.6", "2024-2025", "2024-01-15T09:00:00Z");
    JExam exam2 = saveExam(course.getId(), "0.4", "2024-2025", "2024-02-15T09:00:00Z");
    saveGrade(student.getId(), exam1.getId(), "16", "Exam 1");
    saveGrade(student.getId(), exam2.getId(), "12", "Exam 2");

    ResponseEntity<StudentGeneralAverage> response =
        getAverage(student.getId(), null, adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    StudentGeneralAverage body = response.getBody();
    assertThat(body).isNotNull();
    // course avg: 16 * 0.6 + 12 * 0.4 = 9.6 + 4.8 = 14.4
    // general: 14.4 * 6 / 6 = 14.4
    assertThat(body.generalAverage()).isEqualByComparingTo("14.4");
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
    JUser admin =
        authRepository.save(
            JUser.builder()
                .username("root_admin")
                .password(passwordEncoder.encode("StrongPass12!"))
                .firstName("Root")
                .lastName("Admin")
                .email("root.admin@hacheuil.edu")
                .role(UserRole.ADMIN)
                .build());
    return jwtTokenProvider.generateToken(admin.getId().toString(), admin.getRole().name());
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
    JUser teacher =
        authRepository.save(
            JUser.builder()
                .username("terry_teacher")
                .password(passwordEncoder.encode("StrongPass12!"))
                .firstName("Terry")
                .lastName("Teacher")
                .email("terry.teacher@hacheuil.edu")
                .role(UserRole.TEACHER)
                .teacherInheritance(inheritance)
                .build());
    return jwtTokenProvider.generateToken(teacher.getId().toString(), teacher.getRole().name());
  }

  private ResponseEntity<StudentGeneralAverage> getAverage(
      UUID studentInheritanceId, String academicYear, String token) {
    StringBuilder url =
        new StringBuilder("/reports/students/%s/average".formatted(studentInheritanceId));
    if (academicYear != null) {
      url.append("?academicYear=").append(academicYear);
    }
    return restTemplate.exchange(
        url.toString(),
        HttpMethod.GET,
        new HttpEntity<>(jsonHeaders(token)),
        StudentGeneralAverage.class);
  }

  private ResponseEntity<String> getAverageError(
      UUID studentInheritanceId, String academicYear, String token) {
    StringBuilder url =
        new StringBuilder("/reports/students/%s/average".formatted(studentInheritanceId));
    if (academicYear != null) {
      url.append("?academicYear=").append(academicYear);
    }
    return restTemplate.exchange(
        url.toString(), HttpMethod.GET, new HttpEntity<>(jsonHeaders(token)), String.class);
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
