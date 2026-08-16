package com.techindna.anerti.endpoint.rest.controller.exams;

import static org.assertj.core.api.Assertions.assertThat;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.dto.ExamListResponse;
import com.techindna.anerti.dto.ExamOutput;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.CourseRepository;
import com.techindna.anerti.repository.ExamRepository;
import com.techindna.anerti.repository.TeacherCourseRepository;
import com.techindna.anerti.repository.TeacherInheritanceRepository;
import com.techindna.anerti.repository.enums.TeacherStatus;
import com.techindna.anerti.repository.enums.UserRole;
import com.techindna.anerti.repository.model.JCourse;
import com.techindna.anerti.repository.model.JExam;
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
class GetExamsIT extends FacadeIT {

  private final TestRestTemplate restTemplate;
  private final AuthRepository authRepository;
  private final CourseRepository courseRepository;
  private final ExamRepository examRepository;
  private final TeacherInheritanceRepository teacherInheritanceRepository;
  private final TeacherCourseRepository teacherCourseRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;

  GetExamsIT(
      TestRestTemplate restTemplate,
      AuthRepository authRepository,
      CourseRepository courseRepository,
      ExamRepository examRepository,
      TeacherInheritanceRepository teacherInheritanceRepository,
      TeacherCourseRepository teacherCourseRepository,
      JwtTokenProvider jwtTokenProvider,
      PasswordEncoder passwordEncoder) {
    this.restTemplate = restTemplate;
    this.authRepository = authRepository;
    this.courseRepository = courseRepository;
    this.examRepository = examRepository;
    this.teacherInheritanceRepository = teacherInheritanceRepository;
    this.teacherCourseRepository = teacherCourseRepository;
    this.jwtTokenProvider = jwtTokenProvider;
    this.passwordEncoder = passwordEncoder;
    restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
  }

  @BeforeEach
  void clean() {
    examRepository.deleteAll();
    teacherCourseRepository.deleteAll();
    courseRepository.deleteAll();
    authRepository.deleteAll();
    teacherInheritanceRepository.deleteAll();
  }

  @Test
  void missing_token_is_unauthorized() {
    ResponseEntity<String> response = getExamsError("", null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("Authentication required.");
  }

  @Test
  void admin_lists_all_exams_ordered_by_date() {
    JCourse ep1 = saveCourse("EP1");
    JCourse prj1 = saveCourse("PRJ1");
    saveExam(ep1.getId(), "0.5", "2024-2025", "2024-01-15T09:00:00Z");
    saveExam(prj1.getId(), "1.0", "2024-2025", "2024-01-10T09:00:00Z");
    saveExam(ep1.getId(), "0.5", "2023-2024", "2023-12-01T09:00:00Z");

    ResponseEntity<ExamListResponse> response = getExams("", adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    ExamListResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.meta().page()).isEqualTo(1);
    assertThat(body.meta().size()).isEqualTo(10);
    assertThat(body.meta().total()).isEqualTo(3);
    assertThat(body.data()).hasSize(3);
    assertThat(body.data())
        .extracting(ExamOutput::date)
        .containsExactly(
            Instant.parse("2023-12-01T09:00:00Z"),
            Instant.parse("2024-01-10T09:00:00Z"),
            Instant.parse("2024-01-15T09:00:00Z"));
  }

  @Test
  void student_lists_all_exams() {
    JCourse ep1 = saveCourse("EP1");
    saveExam(ep1.getId(), "0.5", "2024-2025", "2024-01-15T09:00:00Z");

    ResponseEntity<ExamListResponse> response = getExams("", studentToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    ExamListResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.meta().total()).isEqualTo(1);
    assertThat(body.data()).hasSize(1);
    assertThat(body.data().get(0).courseId()).isEqualTo(ep1.getId());
  }

  @Test
  void teacher_only_sees_exams_of_assigned_courses() {
    JCourse ep1 = saveCourse("EP1");
    JCourse prj1 = saveCourse("PRJ1");
    saveExam(ep1.getId(), "0.5", "2024-2025", "2024-01-15T09:00:00Z");
    saveExam(ep1.getId(), "0.5", "2024-2025", "2024-02-15T09:00:00Z");
    saveExam(prj1.getId(), "1.0", "2024-2025", "2024-01-10T09:00:00Z");

    ResponseEntity<ExamListResponse> response = getExams("", teacherTokenWithCourse(ep1));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    ExamListResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.meta().total()).isEqualTo(2);
    assertThat(body.data()).hasSize(2);
    assertThat(body.data())
        .extracting(ExamOutput::courseId)
        .containsExactly(ep1.getId(), ep1.getId());
  }

  @Test
  void teacher_without_assignment_sees_nothing() {
    JCourse ep1 = saveCourse("EP1");
    saveExam(ep1.getId(), "0.5", "2024-2025", "2024-01-15T09:00:00Z");

    ResponseEntity<ExamListResponse> response = getExams("", teacherTokenWithoutCourse());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    ExamListResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.meta().total()).isZero();
    assertThat(body.data()).isEmpty();
  }

  @Test
  void ref_filters_by_course_partial_match() {
    JCourse ep1 = saveCourse("EP1");
    JCourse prj1 = saveCourse("PRJ1");
    saveExam(ep1.getId(), "0.5", "2024-2025", "2024-01-15T09:00:00Z");
    saveExam(ep1.getId(), "0.5", "2024-2025", "2024-02-15T09:00:00Z");
    saveExam(prj1.getId(), "1.0", "2024-2025", "2024-01-10T09:00:00Z");

    ExamListResponse response = getExams("?ref=ep", adminToken()).getBody();

    assertThat(response).isNotNull();
    assertThat(response.meta().total()).isEqualTo(2);
    assertThat(response.data()).hasSize(2);
    assertThat(response.data())
        .extracting(ExamOutput::courseId)
        .containsExactly(ep1.getId(), ep1.getId());
  }

  @Test
  void academic_year_filters_exactly() {
    JCourse ep1 = saveCourse("EP1");
    saveExam(ep1.getId(), "0.5", "2024-2025", "2024-01-15T09:00:00Z");
    saveExam(ep1.getId(), "0.5", "2023-2024", "2023-12-01T09:00:00Z");

    ExamListResponse response = getExams("?academicYear=2024-2025", adminToken()).getBody();

    assertThat(response).isNotNull();
    assertThat(response.meta().total()).isEqualTo(1);
    assertThat(response.data()).hasSize(1);
    assertThat(response.data().get(0).academicYear()).isEqualTo("2024-2025");
  }

  @Test
  void date_range_filters_inclusively() {
    JCourse ep1 = saveCourse("EP1");
    saveExam(ep1.getId(), "0.5", "2024-2025", "2024-01-10T09:00:00Z");
    saveExam(ep1.getId(), "0.5", "2024-2025", "2024-01-15T09:00:00Z");
    saveExam(ep1.getId(), "0.5", "2024-2025", "2024-02-01T09:00:00Z");

    ExamListResponse inclusive =
        getExams("?startDate=2024-01-15T09:00:00Z&endDate=2024-02-01T09:00:00Z", adminToken())
            .getBody();

    assertThat(inclusive).isNotNull();
    assertThat(inclusive.meta().total()).isEqualTo(2);
    assertThat(inclusive.data())
        .extracting(ExamOutput::date)
        .containsExactly(
            Instant.parse("2024-01-15T09:00:00Z"), Instant.parse("2024-02-01T09:00:00Z"));

    ExamListResponse singleBound =
        getExams("?startDate=2024-01-11T09:00:00Z", adminToken()).getBody();
    assertThat(singleBound).isNotNull();
    assertThat(singleBound.meta().total()).isEqualTo(2);
  }

  @Test
  void filters_combine() {
    JCourse ep1 = saveCourse("EP1");
    JCourse prj1 = saveCourse("PRJ1");
    saveExam(ep1.getId(), "0.5", "2024-2025", "2024-01-15T09:00:00Z");
    saveExam(ep1.getId(), "0.5", "2023-2024", "2023-12-01T09:00:00Z");
    saveExam(prj1.getId(), "1.0", "2024-2025", "2024-01-10T09:00:00Z");

    ExamListResponse response = getExams("?ref=ep&academicYear=2024-2025", adminToken()).getBody();

    assertThat(response).isNotNull();
    assertThat(response.meta().total()).isEqualTo(1);
    assertThat(response.data()).hasSize(1);
    assertThat(response.data().get(0).courseId()).isEqualTo(ep1.getId());
  }

  @Test
  void pagination_is_honored() {
    JCourse ep1 = saveCourse("EP1");
    saveExam(ep1.getId(), "0.5", "2024-2025", "2024-01-10T09:00:00Z");
    saveExam(ep1.getId(), "0.5", "2024-2025", "2024-01-11T09:00:00Z");
    saveExam(ep1.getId(), "0.5", "2024-2025", "2024-01-12T09:00:00Z");
    saveExam(ep1.getId(), "0.5", "2024-2025", "2024-01-13T09:00:00Z");
    saveExam(ep1.getId(), "0.5", "2024-2025", "2024-01-14T09:00:00Z");

    ExamListResponse firstPage = getExams("?page=1&size=2", adminToken()).getBody();
    assertThat(firstPage).isNotNull();
    assertThat(firstPage.data()).hasSize(2);
    assertThat(firstPage.meta().page()).isEqualTo(1);
    assertThat(firstPage.meta().size()).isEqualTo(2);
    assertThat(firstPage.meta().total()).isEqualTo(5);

    ExamListResponse secondPage = getExams("?page=2&size=2", adminToken()).getBody();
    assertThat(secondPage).isNotNull();
    assertThat(secondPage.data()).hasSize(2);
    assertThat(secondPage.meta().page()).isEqualTo(2);
    assertThat(secondPage.meta().total()).isEqualTo(5);
    assertThat(firstPage.data().get(0).id()).isNotEqualTo(secondPage.data().get(0).id());
  }

  @Test
  void invalid_page_or_size_falls_back_to_defaults() {
    JCourse ep1 = saveCourse("EP1");
    saveExam(ep1.getId(), "0.5", "2024-2025", "2024-01-10T09:00:00Z");
    saveExam(ep1.getId(), "0.5", "2024-2025", "2024-01-11T09:00:00Z");
    saveExam(ep1.getId(), "0.5", "2024-2025", "2024-01-12T09:00:00Z");

    ExamListResponse negative = getExams("?page=-3&size=-1", adminToken()).getBody();
    assertThat(negative).isNotNull();
    assertThat(negative.meta().page()).isEqualTo(1);
    assertThat(negative.meta().size()).isEqualTo(10);
    assertThat(negative.data()).hasSize(3);

    ExamListResponse tooLarge = getExams("?page=101&size=101", adminToken()).getBody();
    assertThat(tooLarge).isNotNull();
    assertThat(tooLarge.meta().page()).isEqualTo(1);
    assertThat(tooLarge.meta().size()).isEqualTo(10);
    assertThat(tooLarge.data()).hasSize(3);
  }

  @Test
  void invalid_ref_is_unprocessable() {
    ResponseEntity<String> response = getExamsError("?ref=ep1!", adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("Ref ep1! is invalid");
  }

  @Test
  void invalid_date_param_is_bad_request() {
    ResponseEntity<String> response = getExamsError("?startDate=not-a-date", adminToken());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
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

  private String adminToken() {
    JUser admin = saveUser("root_admin", "root.admin@hacheuil.edu", UserRole.ADMIN);
    return jwtTokenProvider.generateToken(admin.getId().toString(), admin.getRole().name());
  }

  private String studentToken() {
    JUser student = saveUser("sam_student", "sam.student@hacheuil.edu", UserRole.STUDENT);
    return jwtTokenProvider.generateToken(student.getId().toString(), student.getRole().name());
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

  private ResponseEntity<ExamListResponse> getExams(String query, String token) {
    return restTemplate.exchange(
        "/exams" + query,
        HttpMethod.GET,
        new HttpEntity<>(jsonHeaders(token)),
        ExamListResponse.class);
  }

  private ResponseEntity<String> getExamsError(String query, String token) {
    return restTemplate.exchange(
        "/exams" + query, HttpMethod.GET, new HttpEntity<>(jsonHeaders(token)), String.class);
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
