package com.techindna.anerti.endpoint.rest.controller.grades;

import static org.assertj.core.api.Assertions.assertThat;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.dto.CourseGradeOutput;
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
class GetComputeCourseGradeIT extends FacadeIT {

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

  GetComputeCourseGradeIT(
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
  void missing_token_is_unauthorized() {
    JStudentInheritance student = saveStudent("S001");
    JCourse ep1 = saveCourse("EP1");

    ResponseEntity<String> response = computeError(student.getId(), ep1.getRef(), null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("Authentication required.");
  }

  @Test
  void admin_computes_weighted_average() {
    JStudentInheritance student = saveStudent("S001");
    JCourse ep1 = saveCourse("EP1");
    JExam exam1 = saveExam(ep1.getId(), "0.50", "2024-2025", "2024-01-15T09:00:00Z");
    JExam exam2 = saveExam(ep1.getId(), "0.50", "2024-2025", "2024-02-15T09:00:00Z");
    saveGrade(student.getId(), exam1.getId(), "14.00", "Midterm");
    saveGrade(student.getId(), exam2.getId(), "16.00", "Final");

    CourseGradeOutput response = compute(student.getId(), ep1.getRef(), adminToken()).getBody();

    assertThat(response).isNotNull();
    assertThat(response.studentInheritanceId()).isEqualTo(student.getId());
    assertThat(response.courseRef()).isEqualTo("EP1");
    // 14.00 * 0.50 + 16.00 * 0.50 = 15.00
    assertThat(response.weightedAverage()).isEqualByComparingTo("15.00");
  }

  @Test
  void student_computes_own_grade() {
    JStudentInheritance student = saveStudent("S001");
    JCourse ep1 = saveCourse("EP1");
    JExam exam = saveExam(ep1.getId(), "1.00", "2024-2025", "2024-01-15T09:00:00Z");
    saveGrade(student.getId(), exam.getId(), "12.50", "Exam");

    JUser studentUser = saveStudentUser(student);
    String token =
        jwtTokenProvider.generateToken(
            studentUser.getId().toString(), studentUser.getRole().name());

    CourseGradeOutput response = compute(student.getId(), ep1.getRef(), token).getBody();

    assertThat(response).isNotNull();
    assertThat(response.weightedAverage()).isEqualByComparingTo("12.50");
  }

  @Test
  void student_computing_other_student_grade_is_forbidden() {
    JStudentInheritance student = saveStudent("S001");
    JStudentInheritance otherStudent = saveStudent("S002");
    JCourse ep1 = saveCourse("EP1");
    JExam exam = saveExam(ep1.getId(), "1.00", "2024-2025", "2024-01-15T09:00:00Z");
    saveGrade(otherStudent.getId(), exam.getId(), "12.50", "Exam");

    JUser studentUser = saveStudentUser(student);
    String token =
        jwtTokenProvider.generateToken(
            studentUser.getId().toString(), studentUser.getRole().name());

    ResponseEntity<String> response = computeError(otherStudent.getId(), ep1.getRef(), token);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void teacher_assigned_to_course_computes() {
    JStudentInheritance student = saveStudent("S001");
    JCourse ep1 = saveCourse("EP1");
    JExam exam = saveExam(ep1.getId(), "1.00", "2024-2025", "2024-01-15T09:00:00Z");
    saveGrade(student.getId(), exam.getId(), "14.00", "Exam");

    CourseGradeOutput response =
        compute(student.getId(), ep1.getRef(), teacherTokenWithCourse(ep1)).getBody();

    assertThat(response).isNotNull();
    assertThat(response.weightedAverage()).isEqualByComparingTo("14.00");
  }

  @Test
  void teacher_not_assigned_to_course_is_forbidden() {
    JStudentInheritance student = saveStudent("S001");
    JCourse ep1 = saveCourse("EP1");
    JExam exam = saveExam(ep1.getId(), "1.00", "2024-2025", "2024-01-15T09:00:00Z");
    saveGrade(student.getId(), exam.getId(), "14.00", "Exam");

    ResponseEntity<String> response =
        computeError(student.getId(), ep1.getRef(), teacherTokenWithoutCourse());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void unknown_student_is_not_found() {
    JCourse ep1 = saveCourse("EP1");
    UUID unknownId = UUID.randomUUID();

    ResponseEntity<String> response = computeError(unknownId, ep1.getRef(), adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody())
        .contains("Student inheritance %s not found".formatted(unknownId));
  }

  @Test
  void unknown_course_is_not_found() {
    JStudentInheritance student = saveStudent("S001");

    ResponseEntity<String> response = computeError(student.getId(), "ZZZ", adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).contains("Course ZZZ not found");
  }

  @Test
  void no_grades_returns_zero() {
    JStudentInheritance student = saveStudent("S001");
    JCourse ep1 = saveCourse("EP1");
    saveExam(ep1.getId(), "0.50", "2024-2025", "2024-01-15T09:00:00Z");

    CourseGradeOutput response = compute(student.getId(), ep1.getRef(), adminToken()).getBody();

    assertThat(response).isNotNull();
    assertThat(response.weightedAverage()).isEqualByComparingTo("0");
  }

  @Test
  void only_exams_with_grades_are_included() {
    JStudentInheritance student = saveStudent("S001");
    JCourse ep1 = saveCourse("EP1");
    JExam exam1 = saveExam(ep1.getId(), "0.50", "2024-2025", "2024-01-15T09:00:00Z");
    JExam exam2 = saveExam(ep1.getId(), "0.50", "2024-2025", "2024-02-15T09:00:00Z");
    saveGrade(student.getId(), exam1.getId(), "16.00", "Midterm");
    // exam2 has no grade

    CourseGradeOutput response = compute(student.getId(), ep1.getRef(), adminToken()).getBody();

    assertThat(response).isNotNull();
    // Only exam1 contributes: 16.00 * 0.50 = 8.00
    assertThat(response.weightedAverage()).isEqualByComparingTo("8.00");
  }

  @Test
  void grade_correction_takes_latest() {
    JStudentInheritance student = saveStudent("S001");
    JCourse ep1 = saveCourse("EP1");
    JExam exam = saveExam(ep1.getId(), "1.00", "2024-2025", "2024-01-15T09:00:00Z");
    saveGrade(student.getId(), exam.getId(), "10.00", "First grade");
    saveGrade(student.getId(), exam.getId(), "18.00", "Corrected grade");

    CourseGradeOutput response = compute(student.getId(), ep1.getRef(), adminToken()).getBody();

    assertThat(response).isNotNull();
    // Latest grade (18.00) wins over 10.00
    assertThat(response.weightedAverage()).isEqualByComparingTo("18.00");
  }

  @Test
  void invalid_course_ref_is_unprocessable() {
    JStudentInheritance student = saveStudent("S001");

    ResponseEntity<String> response = computeError(student.getId(), "ep1!", adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("Ref ep1! is invalid");
  }

  private JCourse saveCourse(String ref) {
    return courseRepository.save(JCourse.builder().ref(ref).title(ref).credits(6).build());
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

  private JStudentInheritance saveStudent(String ref) {
    return studentInheritanceRepository.save(
        JStudentInheritance.builder()
            .ref(ref)
            .level(Level.L2)
            .learningPath(LearningPath.EL)
            .studentStatus(StudentStatus.ACTIVE)
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

  private JUser saveUser(String username, String email, UserRole role) {
    authRepository.findByUsername(username).ifPresent(authRepository::delete);
    authRepository.flush();
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

  private JUser saveStudentUser(JStudentInheritance studentInheritance) {
    return authRepository.save(
        JUser.builder()
            .username("sam_student")
            .password(passwordEncoder.encode("StrongPass12!"))
            .firstName("Sam")
            .lastName("Student")
            .email("sam.student@hacheuil.edu")
            .role(UserRole.STUDENT)
            .studentInheritance(studentInheritance)
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

  private ResponseEntity<CourseGradeOutput> compute(
      UUID studentInheritanceId, String courseRef, String token) {
    return restTemplate.exchange(
        "/grades/compute/%s/course/%s".formatted(studentInheritanceId, courseRef),
        HttpMethod.GET,
        new HttpEntity<>(jsonHeaders(token)),
        CourseGradeOutput.class);
  }

  private ResponseEntity<String> computeError(
      UUID studentInheritanceId, String courseRef, String token) {
    return restTemplate.exchange(
        "/grades/compute/%s/course/%s".formatted(studentInheritanceId, courseRef),
        HttpMethod.GET,
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
