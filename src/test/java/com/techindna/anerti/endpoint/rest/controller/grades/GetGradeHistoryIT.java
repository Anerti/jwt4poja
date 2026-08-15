package com.techindna.anerti.endpoint.rest.controller.grades;

import static org.assertj.core.api.Assertions.assertThat;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.dto.HistoryListResponse;
import com.techindna.anerti.entity.HistoryEntry;
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
class GetGradeHistoryIT extends FacadeIT {

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

  GetGradeHistoryIT(
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
  void history_is_chronological() {
    JStudentInheritance student = saveStudent();
    JExam exam = saveExam(saveCourse("EP1"), "0.5", 2023, "2024-01-15T09:00:00Z");
    JGrade grade = saveGrade(student, exam, "14.5", "Final exam");
    gradeHistoryRepository.save(
        JGradeHistory.builder()
            .gradeId(grade.getId())
            .grade(new BigDecimal("15"))
            .description("Forgot to add bonus points")
            .build());

    ResponseEntity<HistoryListResponse> response =
        restTemplate.exchange(
            "/grades/" + grade.getId() + "/history",
            HttpMethod.GET,
            new HttpEntity<>(jsonHeaders(adminToken())),
            HistoryListResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    HistoryListResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.data()).hasSize(2);
    assertThat(body.data())
        .extracting(HistoryEntry::grade)
        .containsExactly(new BigDecimal("14.5"), new BigDecimal("15"));
    assertThat(body.data().getFirst().description()).isEqualTo("Final exam");
    assertThat(body.data().getLast().description()).isEqualTo("Forgot to add bonus points");
  }

  @Test
  void student_sees_own_grade_history() {
    JStudentInheritance student = saveStudent();
    JExam exam = saveExam(saveCourse("EP1"), "0.5", 2023, "2024-01-15T09:00:00Z");
    JGrade grade = saveGrade(student, exam, "14.5", "Final exam");

    ResponseEntity<HistoryListResponse> response =
        restTemplate.exchange(
            "/grades/" + grade.getId() + "/history",
            HttpMethod.GET,
            new HttpEntity<>(jsonHeaders(studentToken(student))),
            HistoryListResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    HistoryListResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.data()).hasSize(1);
    assertThat(body.data().getFirst().grade()).isEqualByComparingTo(new BigDecimal("14.5"));
  }

  @Test
  void student_cannot_see_other_grade_history() {
    JStudentInheritance studentA = saveStudent();
    JStudentInheritance studentB = saveStudent();
    JExam exam = saveExam(saveCourse("EP1"), "0.5", 2023, "2024-01-15T09:00:00Z");
    JGrade grade = saveGrade(studentA, exam, "14.5", "Final exam");

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/grades/" + grade.getId() + "/history",
            HttpMethod.GET,
            new HttpEntity<>(jsonHeaders(studentToken(studentB))),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody())
        .contains("Student %s cannot access grade %s".formatted(studentB.getId(), grade.getId()));
  }

  @Test
  void unknown_grade_is_not_found() {
    UUID unknownGrade = UUID.randomUUID();

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/grades/" + unknownGrade + "/history",
            HttpMethod.GET,
            new HttpEntity<>(jsonHeaders(adminToken())),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).contains("Grade %s not found".formatted(unknownGrade));
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

  private JGrade saveGrade(
      JStudentInheritance student, JExam exam, String grade, String description) {
    JGrade saved =
        gradeRepository.save(
            JGrade.builder().studentInheritance(student).examId(exam.getId()).build());
    gradeHistoryRepository.save(
        JGradeHistory.builder()
            .gradeId(saved.getId())
            .grade(new BigDecimal(grade))
            .description(description)
            .build());
    return saved;
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

  private String studentToken(JStudentInheritance student) {
    JUser studentUser =
        saveUser("sam_student", "sam.student@hacheuil.edu", UserRole.STUDENT, null, student);
    return jwtTokenProvider.generateToken(
        studentUser.getId().toString(), studentUser.getRole().name());
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
