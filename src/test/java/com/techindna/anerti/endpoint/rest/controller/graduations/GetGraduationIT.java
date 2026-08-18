package com.techindna.anerti.endpoint.rest.controller.graduations;

import static org.assertj.core.api.Assertions.assertThat;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.dto.GraduationListResponse;
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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;

@TestConstructor(autowireMode = AutowireMode.ALL)
class GetGraduationIT extends FacadeIT {

  private final TestRestTemplate restTemplate;
  private final AuthRepository authRepository;
  private final ClassRepository classRepository;
  private final StudentInheritanceRepository studentInheritanceRepository;
  private final CourseRepository courseRepository;
  private final ExamRepository examRepository;
  private final GradeRepository gradeRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;

  GetGraduationIT(
      TestRestTemplate restTemplate,
      AuthRepository authRepository,
      ClassRepository classRepository,
      StudentInheritanceRepository studentInheritanceRepository,
      CourseRepository courseRepository,
      ExamRepository examRepository,
      GradeRepository gradeRepository,
      JwtTokenProvider jwtTokenProvider,
      PasswordEncoder passwordEncoder) {
    this.restTemplate = restTemplate;
    this.authRepository = authRepository;
    this.classRepository = classRepository;
    this.studentInheritanceRepository = studentInheritanceRepository;
    this.courseRepository = courseRepository;
    this.examRepository = examRepository;
    this.gradeRepository = gradeRepository;
    this.jwtTokenProvider = jwtTokenProvider;
    this.passwordEncoder = passwordEncoder;
    restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
  }

  @BeforeEach
  void clean() {
    gradeRepository.deleteAll();
    examRepository.deleteAll();
    authRepository.deleteAll();
    studentInheritanceRepository.deleteAll();
    courseRepository.deleteAll();
    classRepository.deleteAll();
  }

  @Test
  void admin_gets_graduates_for_class() {
    JClass clazz = classRepository.save(JClass.builder().name("Promo 2023").yearOf(2023).build());
    JStudentInheritance student =
        studentInheritanceRepository.save(
            JStudentInheritance.builder()
                .ref("S001")
                .level(Level.L3)
                .learningPath(LearningPath.COMMON)
                .studentStatus(StudentStatus.GRADUATED)
                .classId(clazz.getId())
                .build());
    authRepository.save(
        JUser.builder()
            .username("graduate1")
            .password(passwordEncoder.encode("StrongPass12!"))
            .firstName("Alice")
            .lastName("Dupont")
            .email("alice@hei.edu")
            .role(UserRole.STUDENT)
            .studentInheritance(student)
            .build());

    JCourse course =
        courseRepository.save(JCourse.builder().ref("EP1").title("Course").credits(6).build());
    JExam exam =
        examRepository.save(
            JExam.builder()
                .courseId(course.getId())
                .coefficient(new BigDecimal("0.5"))
                .academicYear("2022-2023")
                .date(Instant.parse("2023-06-15T09:00:00Z"))
                .build());
    gradeRepository.save(
        JGrade.builder()
            .studentInheritanceId(student.getId())
            .examId(exam.getId())
            .value(new BigDecimal("15.50"))
            .description("Final exam")
            .build());

    ResponseEntity<GraduationListResponse> response =
        restTemplate.exchange(
            "/graduations/%s".formatted(clazz.getId()),
            HttpMethod.GET,
            new HttpEntity<>(jsonHeaders(adminToken())),
            GraduationListResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().graduates()).hasSize(1);
    assertThat(response.getBody().graduates().getFirst().firstName()).isEqualTo("Alice");
  }

  @Test
  void student_role_is_forbidden() {
    JClass clazz = classRepository.save(JClass.builder().name("Promo 2023").yearOf(2023).build());

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/graduations/%s".formatted(clazz.getId()),
            HttpMethod.GET,
            new HttpEntity<>(jsonHeaders(studentToken())),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void nonexistent_class_returns_404() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/graduations/%s".formatted(UUID.randomUUID()),
            HttpMethod.GET,
            new HttpEntity<>(jsonHeaders(adminToken())),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  private String adminToken() {
    JUser admin =
        authRepository.save(
            JUser.builder()
                .username("root_admin")
                .password(passwordEncoder.encode("StrongPass12!"))
                .firstName("Root")
                .lastName("Admin")
                .email("admin@hei.edu")
                .role(UserRole.ADMIN)
                .build());
    return jwtTokenProvider.generateToken(admin.getId().toString(), admin.getRole().name());
  }

  private String studentToken() {
    JUser student =
        authRepository.save(
            JUser.builder()
                .username("sam_student")
                .password(passwordEncoder.encode("StrongPass12!"))
                .firstName("Sam")
                .lastName("Student")
                .email("sam@hei.edu")
                .role(UserRole.STUDENT)
                .build());
    return jwtTokenProvider.generateToken(student.getId().toString(), student.getRole().name());
  }

  private HttpHeaders jsonHeaders(String token) {
    HttpHeaders headers = new HttpHeaders();
    if (token != null) {
      headers.setBearerAuth(token);
    }
    return headers;
  }
}
