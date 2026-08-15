package com.techindna.anerti.endpoint.rest.controller.grades;

import static org.assertj.core.api.Assertions.assertThat;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.dto.GradeListResponse;
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
class GetGradesIT extends FacadeIT {

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

  GetGradesIT(
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
  void admin_lists_student_grades() {
    JStudentInheritance student = saveStudent();
    JExam exam1 = saveExam(saveCourse("EP1"), "0.5", 2023, "2024-01-15T09:00:00Z");
    JExam exam2 = saveExam(saveCourse("EP2"), "1", 2024, "2024-06-10T09:00:00Z");
    saveGrade(student, exam1, "14.5");
    saveGrade(student, exam2, "11");

    ResponseEntity<GradeListResponse> response =
        restTemplate.exchange(
            "/grades/" + student.getId(),
            HttpMethod.GET,
            new HttpEntity<>(jsonHeaders(adminToken())),
            GradeListResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    GradeListResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.meta().total()).isEqualTo(2);
    assertThat(body.data())
        .extracting(GradeOutput::examId)
        .containsExactly(exam2.getId(), exam1.getId());
  }

  @Test
  void teacher_only_sees_grades_of_assigned_courses() {
    JTeacherInheritance teacher = saveTeacher();
    JCourse course1 = saveCourse("EP1");
    JCourse course2 = saveCourse("EP2");
    assign(teacher, course1);
    JStudentInheritance student = saveStudent();
    JExam exam1 = saveExam(course1, "0.5", 2023, "2024-01-15T09:00:00Z");
    JExam exam2 = saveExam(course2, "1", 2023, "2024-06-10T09:00:00Z");
    saveGrade(student, exam1, "14.5");
    saveGrade(student, exam2, "11");

    ResponseEntity<GradeListResponse> response =
        restTemplate.exchange(
            "/grades/" + student.getId(),
            HttpMethod.GET,
            new HttpEntity<>(jsonHeaders(teacherToken(teacher))),
            GradeListResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    GradeListResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.meta().total()).isEqualTo(1);
    assertThat(body.data()).extracting(GradeOutput::examId).containsExactly(exam1.getId());
  }

  @Test
  void student_only_sees_own_grades() {
    JStudentInheritance student = saveStudent();
    JStudentInheritance other = saveStudent();
    JExam exam = saveExam(saveCourse("EP1"), "0.5", 2023, "2024-01-15T09:00:00Z");
    saveGrade(student, exam, "14.5");
    saveGrade(other, exam, "12");

    ResponseEntity<GradeListResponse> response =
        restTemplate.exchange(
            "/grades/" + other.getId(),
            HttpMethod.GET,
            new HttpEntity<>(jsonHeaders(studentToken(student))),
            GradeListResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    GradeListResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.meta().total()).isEqualTo(1);
    assertThat(body.data()).extracting(GradeOutput::studentId).containsExactly(student.getId());
  }

  @Test
  void filters_by_exam() {
    JStudentInheritance student = saveStudent();
    JCourse course = saveCourse("EP1");
    JExam exam1 = saveExam(course, "0.5", 2023, "2024-01-15T09:00:00Z");
    JExam exam2 = saveExam(course, "1", 2023, "2024-06-10T09:00:00Z");
    saveGrade(student, exam1, "14.5");
    saveGrade(student, exam2, "11");

    ResponseEntity<GradeListResponse> response =
        restTemplate.exchange(
            "/grades/" + student.getId() + "?examId=" + exam1.getId(),
            HttpMethod.GET,
            new HttpEntity<>(jsonHeaders(adminToken())),
            GradeListResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    GradeListResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.meta().total()).isEqualTo(1);
    assertThat(body.data()).extracting(GradeOutput::examId).containsExactly(exam1.getId());
  }

  @Test
  void filters_by_academic_year() {
    JStudentInheritance student = saveStudent();
    JCourse course = saveCourse("EP1");
    JExam exam2023 = saveExam(course, "0.5", 2023, "2024-01-15T09:00:00Z");
    JExam exam2024 = saveExam(course, "1", 2024, "2024-06-10T09:00:00Z");
    saveGrade(student, exam2023, "14.5");
    saveGrade(student, exam2024, "11");

    ResponseEntity<GradeListResponse> response =
        restTemplate.exchange(
            "/grades/" + student.getId() + "?academicYear=2023",
            HttpMethod.GET,
            new HttpEntity<>(jsonHeaders(adminToken())),
            GradeListResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    GradeListResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.meta().total()).isEqualTo(1);
    assertThat(body.data()).extracting(GradeOutput::examId).containsExactly(exam2023.getId());
  }

  @Test
  void invalid_academic_year_is_unprocessable() {
    JStudentInheritance student = saveStudent();

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/grades/" + student.getId() + "?academicYear=1800",
            HttpMethod.GET,
            new HttpEntity<>(jsonHeaders(adminToken())),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("academicYear must be between 1900 and 2100");
  }

  @Test
  void missing_token_is_unauthorized() {
    JStudentInheritance student = saveStudent();

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/grades/" + student.getId(),
            HttpMethod.GET,
            new HttpEntity<>(jsonHeaders(null)),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("Authentication required.");
  }

  @Test
  void admin_can_list_grades_without_grades() {
    JStudentInheritance student = saveStudent();

    ResponseEntity<GradeListResponse> response =
        restTemplate.exchange(
            "/grades/" + student.getId(),
            HttpMethod.GET,
            new HttpEntity<>(jsonHeaders(adminToken())),
            GradeListResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    GradeListResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.meta().total()).isZero();
    assertThat(body.data()).isEmpty();
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

  private void saveGrade(JStudentInheritance student, JExam exam, String grade) {
    JGrade saved =
        gradeRepository.save(
            JGrade.builder().studentInheritance(student).examId(exam.getId()).build());
    gradeHistoryRepository.save(
        JGradeHistory.builder()
            .gradeId(saved.getId())
            .grade(new BigDecimal(grade))
            .description("")
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

  private HttpHeaders jsonHeaders(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (token != null) {
      headers.setBearerAuth(token);
    }
    return headers;
  }
}
