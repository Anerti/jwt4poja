package com.techindna.anerti.endpoint.rest.controller.users;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.entity.User;
import com.techindna.anerti.entity.enums.UserRole;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.model.JUser;
import com.techindna.anerti.security.jwt.JwtTokenProvider;
import java.util.UUID;
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
class UserGetIT extends FacadeIT {

  private final TestRestTemplate restTemplate;
  private final AuthRepository authRepository;
  private final JwtTokenProvider jwtTokenProvider;

  UserGetIT(
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
  void owner_customer_gets_own_profile() {
    JUser customer = saveUser("jdoe", "John", "Doe", UserRole.CUSTOMER);

    ResponseEntity<User> response = getUser(tokenOf(customer), customer.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    User body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.id()).isEqualTo(customer.getId());
    assertThat(body.username()).isEqualTo("jdoe");
    assertThat(body.firstName()).isEqualTo("John");
    assertThat(body.lastName()).isEqualTo("Doe");
    assertThat(body.email()).isEqualTo("jdoe@example.com");
    assertThat(body.role()).isEqualTo(UserRole.CUSTOMER);
    assertThat(body.createdAt()).isCloseTo(customer.getCreatedAt(), within(1, SECONDS));
  }

  @Test
  void admin_gets_a_customer() {
    JUser customer = saveUser("jdoe", "John", "Doe", UserRole.CUSTOMER);
    String adminToken = adminToken("root");

    ResponseEntity<User> response = getUser(adminToken, customer.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().username()).isEqualTo("jdoe");
  }

  @Test
  void admin_cannot_get_another_admin() {
    JUser otherAdmin = saveUser("boss", "Boss", "Admin", UserRole.ADMIN);
    String adminToken = adminToken("root");

    ResponseEntity<String> response = getUserRaw(adminToken, otherAdmin.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Insufficient privileges");
  }

  @Test
  void customer_cannot_get_another_customer() {
    JUser jdoe = saveUser("jdoe", "John", "Doe", UserRole.CUSTOMER);
    JUser other = saveUser("asmith", "Alice", "Smith", UserRole.CUSTOMER);

    ResponseEntity<String> response = getUserRaw(tokenOf(jdoe), other.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Insufficient privileges");
  }

  @Test
  void missing_token_is_unauthorized() {
    JUser customer = saveUser("jdoe", "John", "Doe", UserRole.CUSTOMER);

    ResponseEntity<String> response = getUserRaw(null, customer.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void unknown_user_is_not_found() {
    saveUser("jdoe", "John", "Doe", UserRole.CUSTOMER);
    String adminToken = adminToken("root");
    UUID unknownId = UUID.randomUUID();

    ResponseEntity<String> response = getUserRaw(adminToken, unknownId);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).contains("User " + unknownId + " not found");
  }

  @Test
  void malformed_uuid_is_bad_request() {
    String adminToken = adminToken("root");

    ResponseEntity<String> response = getUserRaw(adminToken, "not-a-uuid");

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

  private ResponseEntity<User> getUser(String token, UUID userId) {
    return restTemplate.exchange("/users/" + userId, HttpMethod.GET, authorized(token), User.class);
  }

  private ResponseEntity<String> getUserRaw(String token, UUID userId) {
    return getUserRaw(token, userId.toString());
  }

  private ResponseEntity<String> getUserRaw(String token, String userId) {
    return restTemplate.exchange(
        "/users/" + userId, HttpMethod.GET, authorized(token), String.class);
  }

  private HttpEntity<Void> authorized(String token) {
    HttpHeaders headers = new HttpHeaders();
    if (token != null) {
      headers.setBearerAuth(token);
    }
    return new HttpEntity<>(headers);
  }
}
