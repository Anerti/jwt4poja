package com.techindna.anerti.endpoint.rest.controller.users;

import static org.assertj.core.api.Assertions.assertThat;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.dto.UserListResponse;
import com.techindna.anerti.entity.User;
import com.techindna.anerti.entity.enums.UserRole;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.model.JUser;
import com.techindna.anerti.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;

@TestConstructor(autowireMode = AutowireMode.ALL)
class UserListIT extends FacadeIT {

  private final TestRestTemplate restTemplate;
  private final AuthRepository authRepository;
  private final JwtTokenProvider jwtTokenProvider;

  UserListIT(
      TestRestTemplate restTemplate,
      AuthRepository authRepository,
      JwtTokenProvider jwtTokenProvider) {
    this.restTemplate = restTemplate;
    this.authRepository = authRepository;
    this.jwtTokenProvider = jwtTokenProvider;
  }

  @BeforeEach
  void clean() {
    authRepository.deleteAll();
  }

  @Test
  void admin_lists_only_customer_users_with_default_pagination() {
    saveUser("jdoe", "John", "Doe", UserRole.CUSTOMER);
    saveUser("asmith", "Alice", "Smith", UserRole.CUSTOMER);
    String adminToken = adminToken("root");

    ResponseEntity<UserListResponse> response = listUsers(adminToken, null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().meta().page()).isEqualTo(1);
    assertThat(response.getBody().meta().size()).isEqualTo(10);
    assertThat(response.getBody().meta().total()).isEqualTo(2);
    assertThat(response.getBody().data())
        .extracting(User::username)
        .containsExactlyInAnyOrder("jdoe", "asmith");
  }

  @Test
  void search_matches_any_field_case_insensitively() {
    saveUser("jdoe", "John", "Doe", UserRole.CUSTOMER);
    saveUser("asmith", "Alice", "Smith", UserRole.CUSTOMER);
    String adminToken = adminToken("root");

    ResponseEntity<UserListResponse> byUsername = listUsers(adminToken, "search=smith");
    assertThat(byUsername.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(byUsername.getBody().data()).extracting(User::username).containsExactly("asmith");

    ResponseEntity<UserListResponse> byFirstName = listUsers(adminToken, "search=ali");
    assertThat(byFirstName.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(byFirstName.getBody().data()).extracting(User::username).containsExactly("asmith");

    ResponseEntity<UserListResponse> byLastName = listUsers(adminToken, "search=Doe");
    assertThat(byLastName.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(byLastName.getBody().data()).extracting(User::username).containsExactly("jdoe");

    ResponseEntity<UserListResponse> byEmail = listUsers(adminToken, "search=SMITH@EXAMPLE");
    assertThat(byEmail.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(byEmail.getBody().data()).extracting(User::username).containsExactly("asmith");
  }

  @Test
  void page_and_size_are_one_based_and_echoed_in_meta() {
    saveUser("alice", "Alice", "Smith", UserRole.CUSTOMER);
    saveUser("bob", "Bob", "Jones", UserRole.CUSTOMER);
    saveUser("carol", "Carol", "King", UserRole.CUSTOMER);
    String adminToken = adminToken("root");

    ResponseEntity<UserListResponse> response = listUsers(adminToken, "page=2&size=2");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().meta().page()).isEqualTo(2);
    assertThat(response.getBody().meta().size()).isEqualTo(2);
    assertThat(response.getBody().meta().total()).isEqualTo(3);
    assertThat(response.getBody().data()).extracting(User::username).containsExactly("carol");
  }

  @Test
  void sort_applies_created_at_direction() throws InterruptedException {
    saveUser("bob", "Bob", "Jones", UserRole.CUSTOMER);
    Thread.sleep(5);
    saveUser("alice", "Alice", "Smith", UserRole.CUSTOMER);
    String adminToken = adminToken("root");

    ResponseEntity<UserListResponse> asc = listUsers(adminToken, "sort=ASC");
    assertThat(asc.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(asc.getBody().data()).extracting(User::username).containsExactly("bob", "alice");

    ResponseEntity<UserListResponse> desc = listUsers(adminToken, "sort=DESC");
    assertThat(desc.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(desc.getBody().data()).extracting(User::username).containsExactly("alice", "bob");
  }

  @Test
  void no_matching_users_returns_empty_page() {
    String adminToken = adminToken("root");

    ResponseEntity<UserListResponse> response = listUsers(adminToken, "search=zzz");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().data()).isEmpty();
    assertThat(response.getBody().meta().total()).isZero();
  }

  @Test
  void blank_search_returns_all_users() {
    saveUser("jdoe", "John", "Doe", UserRole.CUSTOMER);
    saveUser("asmith", "Alice", "Smith", UserRole.CUSTOMER);
    String adminToken = adminToken("root");

    ResponseEntity<UserListResponse> response = listUsers(adminToken, "search=");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().data())
        .extracting(User::username)
        .containsExactlyInAnyOrder("jdoe", "asmith");
  }

  @Test
  void invalid_search_is_unprocessable() {
    String adminToken = adminToken("root");

    ResponseEntity<String> response = listUsersRaw(adminToken, "search=smith$");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("Search smith$ is invalid");
  }

  @Test
  void missing_token_is_unauthorized() {
    ResponseEntity<String> response = listUsersRaw(null, null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void customer_cannot_list_users() {
    JUser customer = saveUser("jdoe", "John", "Doe", UserRole.CUSTOMER);

    ResponseEntity<String> response = listUsersRaw(tokenOf(customer), null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Insufficient privileges");
  }

  @Test
  void page_below_one_falls_back_to_default() {
    saveUser("jdoe", "John", "Doe", UserRole.CUSTOMER);
    String adminToken = adminToken("root");

    ResponseEntity<UserListResponse> response = listUsers(adminToken, "page=0");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().meta().page()).isEqualTo(1);
    assertThat(response.getBody().meta().size()).isEqualTo(10);
    assertThat(response.getBody().data()).extracting(User::username).containsExactly("jdoe");
  }

  @Test
  void size_below_one_falls_back_to_default() {
    saveUser("jdoe", "John", "Doe", UserRole.CUSTOMER);
    String adminToken = adminToken("root");

    ResponseEntity<UserListResponse> response = listUsers(adminToken, "size=0");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().meta().page()).isEqualTo(1);
    assertThat(response.getBody().meta().size()).isEqualTo(10);
    assertThat(response.getBody().data()).extracting(User::username).containsExactly("jdoe");
  }

  @Test
  void size_above_max_is_capped() {
    saveUser("jdoe", "John", "Doe", UserRole.CUSTOMER);
    String adminToken = adminToken("root");

    ResponseEntity<UserListResponse> response = listUsers(adminToken, "size=1000");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().meta().size()).isEqualTo(100);
    assertThat(response.getBody().data()).extracting(User::username).containsExactly("jdoe");
  }

  @Test
  void invalid_sort_value_is_bad_request() {
    String adminToken = adminToken("root");

    ResponseEntity<String> response = listUsersRaw(adminToken, "sort=sideways");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void non_numeric_page_is_bad_request() {
    String adminToken = adminToken("root");

    ResponseEntity<String> response = listUsersRaw(adminToken, "page=abc");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  private JUser saveUser(String username, String firstName, String lastName, UserRole role) {
    return authRepository.save(
        JUser.builder()
            .username(username)
            .password("encoded-placeholder")
            .firstName(firstName)
            .lastName(lastName)
            .email(username + "@example.com")
            .verified(true)
            .role(role)
            .build());
  }

  private String adminToken(String username) {
    return tokenOf(saveUser(username, "Root", "Admin", UserRole.ADMIN));
  }

  private String tokenOf(JUser user) {
    return jwtTokenProvider.generateToken(user.getId().toString(), user.getRole().name());
  }

  private ResponseEntity<UserListResponse> listUsers(String token, String query) {
    return restTemplate.exchange(
        "/users" + (query == null ? "" : "?" + query),
        HttpMethod.GET,
        authorized(token),
        UserListResponse.class);
  }

  private ResponseEntity<String> listUsersRaw(String token, String query) {
    return restTemplate.exchange(
        "/users" + (query == null ? "" : "?" + query),
        HttpMethod.GET,
        authorized(token),
        String.class);
  }

  private HttpEntity<Void> authorized(String token) {
    HttpHeaders headers = new HttpHeaders();
    if (token != null) {
      headers.setBearerAuth(token);
    }
    return new HttpEntity<>(headers);
  }
}
