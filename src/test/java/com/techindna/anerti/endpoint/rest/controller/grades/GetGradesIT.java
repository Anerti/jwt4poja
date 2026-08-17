package com.techindna.anerti.endpoint.rest.controller.grades;

import static org.assertj.core.api.Assertions.assertThat;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.dto.GradeListResponse;
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
class GetGradesIT extends FacadeIT {

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

  GetGradesIT(
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

    ResponseEntity<String> response = getGradesError(student.getId(), "", null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("Authentication required.");
  }

  @Test
  void admin_lists_all_grades_ordered_by_created_at_desc() {
    JStudentInheritance student = saveStudent("S001");
    JCourse ep1 = saveCourse("EP1");
    JCourse prj1 = saveCourse("PRJ1");
    JExam exam1 = saveExam(ep1.getId(), "0.5", "2024-2025", "2024-01-15T09:00:00Z");
    JExam exam2 = saveExam(prj1.getId(), "0.5", "2024-2025", "2024-01-10T09:00:00Z");
    saveGrade(student.getId(), exam1.getId(), "14.50", "First exam");
    saveGrade(student.getId(), exam2.getId(), "12.00", "Second exam");

    GradeListResponse response = getGrades(student.getId(), "", adminToken()).getBody();

    assertThat(response).isNotNull();
    assertThat(response.meta().total()).isEqualTo(2);
    assertThat(response.data()).hasSize(2);
    assertThat(response.data().get(0).value()).isEqualByComparingTo("12.00");
    assertThat(response.data().get(1).value()).isEqualByComparingTo("14.50");
  }

  @Test
  void student_can_only_list_own_grades() {
    JStudentInheritance student = saveStudent("S001");
    JCourse ep1 = saveCourse("EP1");
    JExam exam = saveExam(ep1.getId(), "0.5", "2024-2025", "2024-01-15T09:00:00Z");
    saveGrade(student.getId(), exam.getId(), "14.50", "First exam");

    JUser studentUser = saveStudentUser(student);
    String token =
        jwtTokenProvider.generateToken(
            studentUser.getId().toString(), studentUser.getRole().name());

    GradeListResponse response = getGrades(student.getId(), "", token).getBody();

    assertThat(response).isNotNull();
    assertThat(response.meta().total()).isEqualTo(1);
    assertThat(response.data()).hasSize(1);
  }

  @Test
  void student_trying_to_view_other_grades_is_forbidden() {
    JStudentInheritance student = saveStudent("S001");
    JStudentInheritance otherStudent = saveStudent("S002");
    JCourse ep1 = saveCourse("EP1");
    JExam exam = saveExam(ep1.getId(), "0.5", "2024-2025", "2024-01-15T09:00:00Z");
    saveGrade(otherStudent.getId(), exam.getId(), "14.50", "First exam");

    JUser studentUser = saveStudentUser(student);
    String token =
        jwtTokenProvider.generateToken(
            studentUser.getId().toString(), studentUser.getRole().name());

    ResponseEntity<String> response = getGradesError(otherStudent.getId(), "", token);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Students can only view their own grades.");
  }

  @Test
  void teacher_only_sees_grades_of_assigned_courses() {
    JStudentInheritance student = saveStudent("S001");
    JCourse ep1 = saveCourse("EP1");
    JCourse prj1 = saveCourse("PRJ1");
    JExam exam1 = saveExam(ep1.getId(), "0.5", "2024-2025", "2024-01-15T09:00:00Z");
    JExam exam2 = saveExam(prj1.getId(), "0.5", "2024-2025", "2024-01-10T09:00:00Z");
    saveGrade(student.getId(), exam1.getId(), "14.50", "First exam");
    saveGrade(student.getId(), exam2.getId(), "12.00", "Second exam");

    GradeListResponse response =
        getGrades(student.getId(), "", teacherTokenWithCourse(ep1)).getBody();

    assertThat(response).isNotNull();
    assertThat(response.meta().total()).isEqualTo(1);
    assertThat(response.data()).hasSize(1);
    assertThat(response.data().get(0).examId()).isEqualTo(exam1.getId());
  }

  @Test
  void teacher_without_assignment_sees_nothing() {
    JStudentInheritance student = saveStudent("S001");
    JCourse ep1 = saveCourse("EP1");
    JExam exam = saveExam(ep1.getId(), "0.5", "2024-2025", "2024-01-15T09:00:00Z");
    saveGrade(student.getId(), exam.getId(), "14.50", "First exam");

    GradeListResponse response =
        getGrades(student.getId(), "", teacherTokenWithoutCourse()).getBody();

    assertThat(response).isNotNull();
    assertThat(response.meta().total()).isZero();
    assertThat(response.data()).isEmpty();
  }

  @Test
  void course_ref_filters_by_partial_match() {
    JStudentInheritance student = saveStudent("S001");
    JCourse ep1 = saveCourse("EP1");
    JCourse prj1 = saveCourse("PRJ1");
    JExam exam1 = saveExam(ep1.getId(), "0.5", "2024-2025", "2024-01-15T09:00:00Z");
    JExam exam2 = saveExam(prj1.getId(), "0.5", "2024-2025", "2024-01-10T09:00:00Z");
    saveGrade(student.getId(), exam1.getId(), "14.50", "First exam");
    saveGrade(student.getId(), exam2.getId(), "12.00", "Second exam");

    GradeListResponse response =
        getGrades(student.getId(), "?courseRef=ep", adminToken()).getBody();

    assertThat(response).isNotNull();
    assertThat(response.meta().total()).isEqualTo(1);
    assertThat(response.data()).hasSize(1);
    assertThat(response.data().get(0).examId()).isEqualTo(exam1.getId());
  }

  @Test
  void academic_year_filters_exactly() {
    JStudentInheritance student = saveStudent("S001");
    JCourse ep1 = saveCourse("EP1");
    JExam exam1 = saveExam(ep1.getId(), "0.5", "2024-2025", "2024-01-15T09:00:00Z");
    JExam exam2 = saveExam(ep1.getId(), "0.5", "2023-2024", "2023-12-01T09:00:00Z");
    saveGrade(student.getId(), exam1.getId(), "14.50", "First exam");
    saveGrade(student.getId(), exam2.getId(), "12.00", "Second exam");

    GradeListResponse response =
        getGrades(student.getId(), "?academicYear=2024-2025", adminToken()).getBody();

    assertThat(response).isNotNull();
    assertThat(response.meta().total()).isEqualTo(1);
    assertThat(response.data()).hasSize(1);
  }

  @Test
  void filters_combine() {
    JStudentInheritance student = saveStudent("S001");
    JCourse ep1 = saveCourse("EP1");
    JCourse prj1 = saveCourse("PRJ1");
    JExam exam1 = saveExam(ep1.getId(), "0.5", "2024-2025", "2024-01-15T09:00:00Z");
    JExam exam2 = saveExam(ep1.getId(), "0.5", "2023-2024", "2023-12-01T09:00:00Z");
    JExam exam3 = saveExam(prj1.getId(), "0.5", "2024-2025", "2024-01-10T09:00:00Z");
    saveGrade(student.getId(), exam1.getId(), "14.50", "First exam");
    saveGrade(student.getId(), exam2.getId(), "12.00", "Second exam");
    saveGrade(student.getId(), exam3.getId(), "15.00", "Third exam");

    GradeListResponse response =
        getGrades(student.getId(), "?courseRef=ep&academicYear=2024-2025", adminToken()).getBody();

    assertThat(response).isNotNull();
    assertThat(response.meta().total()).isEqualTo(1);
    assertThat(response.data()).hasSize(1);
    assertThat(response.data().get(0).examId()).isEqualTo(exam1.getId());
  }

  @Test
  void pagination_is_honored() {
    JStudentInheritance student = saveStudent("S001");
    JCourse ep1 = saveCourse("EP1");
    for (int i = 0; i < 5; i++) {
      JExam exam =
          saveExam(ep1.getId(), "0.5", "2024-2025", "2024-01-%02dT09:00:00Z".formatted(10 + i));
      saveGrade(student.getId(), exam.getId(), "10.00", "Exam " + i);
    }

    GradeListResponse firstPage =
        getGrades(student.getId(), "?page=1&size=2", adminToken()).getBody();
    assertThat(firstPage).isNotNull();
    assertThat(firstPage.data()).hasSize(2);
    assertThat(firstPage.meta().page()).isEqualTo(1);
    assertThat(firstPage.meta().size()).isEqualTo(2);
    assertThat(firstPage.meta().total()).isEqualTo(5);

    GradeListResponse secondPage =
        getGrades(student.getId(), "?page=2&size=2", adminToken()).getBody();
    assertThat(secondPage).isNotNull();
    assertThat(secondPage.data()).hasSize(2);
    assertThat(secondPage.meta().page()).isEqualTo(2);
    assertThat(secondPage.meta().total()).isEqualTo(5);
    assertThat(firstPage.data().get(0).id()).isNotEqualTo(secondPage.data().get(0).id());
  }

  @Test
  void invalid_page_or_size_falls_back_to_defaults() {
    JStudentInheritance student = saveStudent("S001");
    JCourse ep1 = saveCourse("EP1");
    for (int i = 0; i < 3; i++) {
      JExam exam =
          saveExam(ep1.getId(), "0.5", "2024-2025", "2024-01-%02dT09:00:00Z".formatted(10 + i));
      saveGrade(student.getId(), exam.getId(), "10.00", "Exam " + i);
    }

    GradeListResponse negative =
        getGrades(student.getId(), "?page=-3&size=-1", adminToken()).getBody();
    assertThat(negative).isNotNull();
    assertThat(negative.meta().page()).isEqualTo(1);
    assertThat(negative.meta().size()).isEqualTo(10);
    assertThat(negative.data()).hasSize(3);

    GradeListResponse tooLarge =
        getGrades(student.getId(), "?page=101&size=101", adminToken()).getBody();
    assertThat(tooLarge).isNotNull();
    assertThat(tooLarge.meta().page()).isEqualTo(1);
    assertThat(tooLarge.meta().size()).isEqualTo(10);
    assertThat(tooLarge.data()).hasSize(3);
  }

  @Test
  void invalid_ref_is_unprocessable() {
    JStudentInheritance student = saveStudent("S001");

    ResponseEntity<String> response =
        getGradesError(student.getId(), "?courseRef=ep1!", adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("Ref ep1! is invalid");
  }

  @Test
  void unknown_student_is_not_found() {
    UUID unknownId = UUID.randomUUID();

    ResponseEntity<String> response = getGradesError(unknownId, "", adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody())
        .contains("Student inheritance %s not found".formatted(unknownId));
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

  private ResponseEntity<GradeListResponse> getGrades(
      UUID studentInheritanceId, String query, String token) {
    return restTemplate.exchange(
        "/grades/%s%s".formatted(studentInheritanceId, query),
        HttpMethod.GET,
        new HttpEntity<>(jsonHeaders(token)),
        GradeListResponse.class);
  }

  private ResponseEntity<String> getGradesError(
      UUID studentInheritanceId, String query, String token) {
    return restTemplate.exchange(
        "/grades/%s%s".formatted(studentInheritanceId, query),
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
