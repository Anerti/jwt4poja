package com.techindna.anerti.endpoint.rest.controller.reports;

import static org.assertj.core.api.Assertions.assertThat;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.ClassRepository;
import com.techindna.anerti.repository.CourseRepository;
import com.techindna.anerti.repository.ExamRepository;
import com.techindna.anerti.repository.GradeRepository;
import com.techindna.anerti.repository.StudentInheritanceRepository;
import com.techindna.anerti.repository.TeacherCourseRepository;
import com.techindna.anerti.repository.TeacherInheritanceRepository;
import com.techindna.anerti.repository.enums.LearningPath;
import com.techindna.anerti.repository.enums.Level;
import com.techindna.anerti.repository.enums.StudentStatus;
import com.techindna.anerti.repository.enums.UserRole;
import com.techindna.anerti.repository.model.JClass;
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
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
class DownloadClassRankingIT extends FacadeIT {

  private final TestRestTemplate restTemplate;
  private final AuthRepository authRepository;
  private final ClassRepository classRepository;
  private final StudentInheritanceRepository studentInheritanceRepository;
  private final TeacherInheritanceRepository teacherInheritanceRepository;
  private final CourseRepository courseRepository;
  private final ExamRepository examRepository;
  private final GradeRepository gradeRepository;
  private final TeacherCourseRepository teacherCourseRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;

  DownloadClassRankingIT(
      TestRestTemplate restTemplate,
      AuthRepository authRepository,
      ClassRepository classRepository,
      StudentInheritanceRepository studentInheritanceRepository,
      TeacherInheritanceRepository teacherInheritanceRepository,
      CourseRepository courseRepository,
      ExamRepository examRepository,
      GradeRepository gradeRepository,
      TeacherCourseRepository teacherCourseRepository,
      JwtTokenProvider jwtTokenProvider,
      PasswordEncoder passwordEncoder) {
    this.restTemplate = restTemplate;
    this.authRepository = authRepository;
    this.classRepository = classRepository;
    this.studentInheritanceRepository = studentInheritanceRepository;
    this.teacherInheritanceRepository = teacherInheritanceRepository;
    this.courseRepository = courseRepository;
    this.examRepository = examRepository;
    this.gradeRepository = gradeRepository;
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
    authRepository.deleteAll();
    studentInheritanceRepository.deleteAll();
    teacherInheritanceRepository.deleteAll();
    courseRepository.deleteAll();
    classRepository.deleteAll();
  }

  @Test
  void admin_downloads_class_ranking_xlsx() throws Exception {
    JClass clazz = classRepository.save(JClass.builder().name("L2 EL 2024").yearOf(2024).build());
    JStudentInheritance student =
        studentInheritanceRepository.save(
            JStudentInheritance.builder()
                .ref("S001")
                .level(Level.L2)
                .learningPath(LearningPath.EL)
                .studentStatus(StudentStatus.ACTIVE)
                .classId(clazz.getId())
                .build());
    authRepository.save(
        JUser.builder()
            .username("student1")
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
                .academicYear("2023-2024")
                .date(Instant.parse("2024-01-15T09:00:00Z"))
                .build());
    gradeRepository.save(
        JGrade.builder()
            .studentInheritanceId(student.getId())
            .examId(exam.getId())
            .value(new BigDecimal("15.50"))
            .description("Final exam")
            .build());

    ResponseEntity<byte[]> response =
        restTemplate.exchange(
            "/reports/classes/%s/ranking/download".formatted(clazz.getId()),
            HttpMethod.GET,
            new HttpEntity<>(xlsxHeaders(adminToken())),
            byte[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType().toString())
        .contains("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
        .contains("attachment");
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().length).isGreaterThan(0);

    try (Workbook workbook =
        new XSSFWorkbook(new java.io.ByteArrayInputStream(response.getBody()))) {
      Sheet sheet = workbook.getSheetAt(0);
      assertThat(sheet.getSheetName()).isEqualTo("Ranking");

      Row header = sheet.getRow(0);
      assertThat(header.getCell(0).getStringCellValue()).isEqualTo("Rank");
      assertThat(header.getCell(1).getStringCellValue()).isEqualTo("Student Ref");
      assertThat(header.getCell(2).getStringCellValue()).isEqualTo("First Name");
      assertThat(header.getCell(3).getStringCellValue()).isEqualTo("Last Name");
      assertThat(header.getCell(4).getStringCellValue()).isEqualTo("General Average");

      Row data = sheet.getRow(1);
      assertThat(data.getCell(0).getNumericCellValue()).isEqualTo(1);
      assertThat(data.getCell(1).getStringCellValue()).isEqualTo("S001");
      assertThat(data.getCell(2).getStringCellValue()).isEqualTo("Alice");
      assertThat(data.getCell(3).getStringCellValue()).isEqualTo("Dupont");
    }
  }

  @Test
  void teacher_can_download_ranking() {
    JClass clazz = classRepository.save(JClass.builder().name("L2 TN 2024").yearOf(2024).build());
    JStudentInheritance student =
        studentInheritanceRepository.save(
            JStudentInheritance.builder()
                .ref("S002")
                .level(Level.L2)
                .learningPath(LearningPath.TN)
                .studentStatus(StudentStatus.ACTIVE)
                .classId(clazz.getId())
                .build());
    authRepository.save(
        JUser.builder()
            .username("student2")
            .password(passwordEncoder.encode("StrongPass12!"))
            .firstName("Bob")
            .lastName("Martin")
            .email("bob@hei.edu")
            .role(UserRole.STUDENT)
            .studentInheritance(student)
            .build());

    JCourse course =
        courseRepository.save(JCourse.builder().ref("PRJ1").title("Project").credits(4).build());
    JExam exam =
        examRepository.save(
            JExam.builder()
                .courseId(course.getId())
                .coefficient(new BigDecimal("1.0"))
                .academicYear("2023-2024")
                .date(Instant.parse("2024-01-20T09:00:00Z"))
                .build());
    gradeRepository.save(
        JGrade.builder()
            .studentInheritanceId(student.getId())
            .examId(exam.getId())
            .value(new BigDecimal("12.00"))
            .description("Project evaluation")
            .build());

    JTeacherInheritance teacher =
        teacherInheritanceRepository.save(JTeacherInheritance.builder().ref("T001").build());
    authRepository.save(
        JUser.builder()
            .username("teacher1")
            .password(passwordEncoder.encode("StrongPass12!"))
            .firstName("Paul")
            .lastName("Martin")
            .email("paul@hei.edu")
            .role(UserRole.TEACHER)
            .teacherInheritance(teacher)
            .build());
    teacherCourseRepository.save(
        JTeacherCourse.builder()
            .teacherInheritance(teacher)
            .courseId(course.getId())
            .assignedAt(Instant.parse("2023-09-01T08:00:00Z"))
            .build());

    ResponseEntity<byte[]> response =
        restTemplate.exchange(
            "/reports/classes/%s/ranking/download".formatted(clazz.getId()),
            HttpMethod.GET,
            new HttpEntity<>(xlsxHeaders(teacherToken(teacher))),
            byte[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().length).isGreaterThan(0);
  }

  @Test
  void student_role_is_forbidden() {
    JClass clazz = classRepository.save(JClass.builder().name("L2 EL 2024").yearOf(2024).build());

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/reports/classes/%s/ranking/download".formatted(clazz.getId()),
            HttpMethod.GET,
            new HttpEntity<>(xlsxHeaders(studentToken())),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void nonexistent_class_returns_404() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/reports/classes/%s/ranking/download".formatted(UUID.randomUUID()),
            HttpMethod.GET,
            new HttpEntity<>(xlsxHeaders(adminToken())),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void empty_class_returns_xlsx_with_only_header() throws Exception {
    JClass clazz = classRepository.save(JClass.builder().name("L2 EL 2024").yearOf(2024).build());

    ResponseEntity<byte[]> response =
        restTemplate.exchange(
            "/reports/classes/%s/ranking/download".formatted(clazz.getId()),
            HttpMethod.GET,
            new HttpEntity<>(xlsxHeaders(adminToken())),
            byte[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();

    try (Workbook workbook =
        new XSSFWorkbook(new java.io.ByteArrayInputStream(response.getBody()))) {
      Sheet sheet = workbook.getSheetAt(0);
      assertThat(sheet.getLastRowNum()).isEqualTo(0);
      Row header = sheet.getRow(0);
      assertThat(header.getCell(0).getStringCellValue()).isEqualTo("Rank");
    }
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

  private String teacherToken(JTeacherInheritance teacher) {
    JUser teacherUser =
        authRepository.findAll().stream()
            .filter(
                u ->
                    u.getTeacherInheritance() != null
                        && u.getTeacherInheritance().getId().equals(teacher.getId()))
            .findFirst()
            .orElseThrow();
    return jwtTokenProvider.generateToken(
        teacherUser.getId().toString(), teacherUser.getRole().name());
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

  private HttpHeaders xlsxHeaders(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(
        java.util.List.of(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")));
    if (token != null) {
      headers.setBearerAuth(token);
    }
    return headers;
  }
}
