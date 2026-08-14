package com.techindna.anerti.endpoint.rest.controller.courses;

import static org.assertj.core.api.Assertions.assertThat;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.dto.CourseListResponse;
import com.techindna.anerti.dto.CourseOutput;
import com.techindna.anerti.repository.CourseRepository;
import com.techindna.anerti.repository.enums.CourseType;
import com.techindna.anerti.repository.model.JCourse;
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
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;

@TestConstructor(autowireMode = AutowireMode.ALL)
class GetCoursesIT extends FacadeIT {

  private final TestRestTemplate restTemplate;
  private final CourseRepository courseRepository;

  GetCoursesIT(TestRestTemplate restTemplate, CourseRepository courseRepository) {
    this.restTemplate = restTemplate;
    this.courseRepository = courseRepository;
    restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
  }

  @BeforeEach
  void clean() {
    courseRepository.deleteAll();
  }

  @Test
  void lists_all_courses_with_meta_without_token() {
    saveCourse("EP1", "Digital Electronics", CourseType.EL, 6);
    saveCourse("PRJ1", "Project Management", CourseType.TN, 4);
    saveCourse("COM1", "Communication", CourseType.COMMON, 2);

    ResponseEntity<CourseListResponse> response = getCourses("");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    CourseListResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.meta().page()).isEqualTo(1);
    assertThat(body.meta().size()).isEqualTo(10);
    assertThat(body.meta().total()).isEqualTo(3);
    assertThat(body.data()).hasSize(3);
    assertThat(body.data())
        .extracting(CourseOutput::ref)
        .containsExactlyInAnyOrder("EP1", "PRJ1", "COM1");
  }

  @Test
  void public_endpoint_needs_no_token() {
    saveCourse("EP1", "Digital Electronics", CourseType.EL, 6);

    ResponseEntity<String> response = getCoursesError("");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("Digital Electronics");
  }

  @Test
  void search_matches_ref_and_title_case_insensitive() {
    saveCourse("EP1", "Digital Electronics", CourseType.EL, 6);
    saveCourse("EP2", "Electronics Lab", CourseType.EL, 3);
    saveCourse("PRJ1", "Project Management", CourseType.TN, 4);

    CourseListResponse byTitle = getCourses("?search=electronics").getBody();
    assertThat(byTitle).isNotNull();
    assertThat(byTitle.data()).hasSize(2);
    assertThat(byTitle.data())
        .extracting(CourseOutput::ref)
        .containsExactlyInAnyOrder("EP1", "EP2");

    CourseListResponse byRef = getCourses("?search=prj").getBody();
    assertThat(byRef).isNotNull();
    assertThat(byRef.data()).hasSize(1);
    assertThat(byRef.data().get(0).ref()).isEqualTo("PRJ1");
  }

  @Test
  void type_filters_exactly() {
    saveCourse("EP1", "Digital Electronics", CourseType.EL, 6);
    saveCourse("PRJ1", "Project Management", CourseType.TN, 4);
    saveCourse("COM1", "Communication", CourseType.COMMON, 2);

    CourseListResponse response = getCourses("?type=COMMON").getBody();

    assertThat(response).isNotNull();
    assertThat(response.meta().total()).isEqualTo(1);
    assertThat(response.data()).hasSize(1);
    assertThat(response.data().get(0).ref()).isEqualTo("COM1");
  }

  @Test
  void search_and_type_combine() {
    saveCourse("EP1", "Digital Electronics", CourseType.EL, 6);
    saveCourse("EP2", "Electronics Lab", CourseType.EL, 3);
    saveCourse("PRJ1", "Electronics Project", CourseType.TN, 4);

    CourseListResponse response = getCourses("?search=electronics&type=EL").getBody();

    assertThat(response).isNotNull();
    assertThat(response.meta().total()).isEqualTo(2);
    assertThat(response.data())
        .extracting(CourseOutput::ref)
        .containsExactlyInAnyOrder("EP1", "EP2");
  }

  @Test
  void pagination_is_honored() {
    saveCourse("EP1", "Digital Electronics", CourseType.EL, 6);
    saveCourse("EP2", "Electronics Lab", CourseType.EL, 3);
    saveCourse("PRJ1", "Project Management", CourseType.TN, 4);
    saveCourse("COM1", "Communication", CourseType.COMMON, 2);
    saveCourse("MATH1", "Mathematics", CourseType.TN, 5);

    CourseListResponse firstPage = getCourses("?page=1&size=2").getBody();
    assertThat(firstPage).isNotNull();
    assertThat(firstPage.data()).hasSize(2);
    assertThat(firstPage.meta().page()).isEqualTo(1);
    assertThat(firstPage.meta().size()).isEqualTo(2);
    assertThat(firstPage.meta().total()).isEqualTo(5);

    CourseListResponse secondPage = getCourses("?page=2&size=2").getBody();
    assertThat(secondPage).isNotNull();
    assertThat(secondPage.data()).hasSize(2);
    assertThat(secondPage.meta().page()).isEqualTo(2);
    assertThat(secondPage.meta().total()).isEqualTo(5);
    assertThat(firstPage.data().get(0).ref()).isNotEqualTo(secondPage.data().get(0).ref());

    CourseListResponse all = getCourses("?size=100").getBody();
    assertThat(all).isNotNull();
    assertThat(all.data()).hasSize(5);
    assertThat(all.meta().total()).isEqualTo(5);
  }

  @Test
  void invalid_page_or_size_falls_back_to_defaults() {
    saveCourse("EP1", "Digital Electronics", CourseType.EL, 6);
    saveCourse("EP2", "Electronics Lab", CourseType.EL, 3);
    saveCourse("PRJ1", "Project Management", CourseType.TN, 4);
    saveCourse("COM1", "Communication", CourseType.COMMON, 2);
    saveCourse("MATH1", "Mathematics", CourseType.TN, 5);

    CourseListResponse negative = getCourses("?page=-3&size=-1").getBody();
    assertThat(negative).isNotNull();
    assertThat(negative.meta().page()).isEqualTo(1);
    assertThat(negative.meta().size()).isEqualTo(10);
    assertThat(negative.data()).hasSize(5);

    CourseListResponse zero = getCourses("?page=0&size=0").getBody();
    assertThat(zero).isNotNull();
    assertThat(zero.meta().page()).isEqualTo(1);
    assertThat(zero.meta().size()).isEqualTo(10);

    CourseListResponse tooLarge = getCourses("?page=101&size=101").getBody();
    assertThat(tooLarge).isNotNull();
    assertThat(tooLarge.meta().page()).isEqualTo(1);
    assertThat(tooLarge.meta().size()).isEqualTo(10);
    assertThat(tooLarge.data()).hasSize(5);
  }

  @Test
  void invalid_type_is_bad_request() {
    ResponseEntity<String> response = getCoursesError("?type=NOPE");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("Invalid parameter: type");
  }

  @Test
  void invalid_search_chars_are_unprocessable() {
    ResponseEntity<String> response = getCoursesError("?search=ep1!");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("Search ep1! is invalid");
  }

  private JCourse saveCourse(String ref, String title, CourseType type, int credits) {
    return courseRepository.save(
        JCourse.builder().ref(ref).title(title).type(type).credits(credits).build());
  }

  private ResponseEntity<CourseListResponse> getCourses(String query) {
    return restTemplate.exchange(
        "/courses" + query,
        HttpMethod.GET,
        new HttpEntity<>(jsonHeaders()),
        CourseListResponse.class);
  }

  private ResponseEntity<String> getCoursesError(String query) {
    return restTemplate.exchange(
        "/courses" + query, HttpMethod.GET, new HttpEntity<>(jsonHeaders()), String.class);
  }

  private HttpHeaders jsonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }
}
