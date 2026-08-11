package com.techindna.anerti.endpoint.rest.controller.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.dto.LoginInput;
import com.techindna.anerti.dto.MessageBody;
import com.techindna.anerti.endpoint.event.EventProducer;
import com.techindna.anerti.endpoint.event.model.SendEmailRequested;
import com.techindna.anerti.entity.enums.UserRole;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.model.JUser;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
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
class LoginIT extends FacadeIT {

  private final TestRestTemplate restTemplate;
  private final AuthRepository authRepository;
  private final PasswordEncoder passwordEncoder;
  private final StringRedisTemplate redis;

  @MockBean private EventProducer<SendEmailRequested> eventProducer;

  LoginIT(
      TestRestTemplate restTemplate,
      AuthRepository authRepository,
      PasswordEncoder passwordEncoder,
      StringRedisTemplate redis) {
    this.restTemplate = restTemplate;
    this.authRepository = authRepository;
    this.passwordEncoder = passwordEncoder;
    this.redis = redis;
    restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
  }

  @BeforeEach
  void clean() {
    authRepository.deleteAll();
    redis.keys("verification:*").forEach(redis::delete);
  }

  @Test
  void valid_login_by_email_creates_token_and_sends_login_verification_email() {
    saveUser("jane_doe", "jane.doe@example.com", "StrongPass12!", true);

    ResponseEntity<MessageBody> response =
        login(new LoginInput(null, "jane.doe@example.com", "StrongPass12!"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(response.getBody().message())
        .isEqualTo("A verification link has been sent to your email");

    String token = singleVerificationToken();
    assertThat(redis.opsForValue().get("verification:" + token)).isEqualTo("jane.doe@example.com");

    SendEmailRequested event = capturedEmailEvent();
    assertThat(event.getTo()).isEqualTo("jane.doe@example.com");
    assertThat(event.getSubject()).isEqualTo("Login Verification");
    assertThat(event.getHtmlBody()).contains("/auth/verification/" + token);
  }

  @Test
  void valid_login_by_username_creates_token_and_sends_login_verification_email() {
    saveUser("jane_doe", "jane.doe@example.com", "StrongPass12!", true);

    ResponseEntity<MessageBody> response = login(new LoginInput("jane_doe", null, "StrongPass12!"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

    String token = singleVerificationToken();
    assertThat(redis.opsForValue().get("verification:" + token)).isEqualTo("jane.doe@example.com");

    SendEmailRequested event = capturedEmailEvent();
    assertThat(event.getTo()).isEqualTo("jane.doe@example.com");
    assertThat(event.getSubject()).isEqualTo("Login Verification");
    assertThat(event.getHtmlBody()).contains("/auth/verification/" + token);
  }

  @Test
  void login_normalizes_email_to_lowercase() {
    saveUser("jane_doe", "jane.doe@example.com", "StrongPass12!", true);

    ResponseEntity<MessageBody> response =
        login(new LoginInput(null, "Jane.Doe@Example.COM", "StrongPass12!"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    String token = singleVerificationToken();
    assertThat(redis.opsForValue().get("verification:" + token)).isEqualTo("jane.doe@example.com");
  }

  @Test
  void wrong_password_is_unauthorized() {
    saveUser("jane_doe", "jane.doe@example.com", "StrongPass12!", true);

    ResponseEntity<String> response =
        loginError(new LoginInput(null, "jane.doe@example.com", "WrongPass12!"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("Invalid credentials");
    assertThat(redis.keys("verification:*")).isEmpty();
    verify(eventProducer, never()).accept(any());
  }

  @Test
  void unknown_email_is_unauthorized() {
    ResponseEntity<String> response =
        loginError(new LoginInput(null, "ghost@example.com", "StrongPass12!"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("Invalid credentials");
    assertThat(redis.keys("verification:*")).isEmpty();
    verify(eventProducer, never()).accept(any());
  }

  @Test
  void unknown_username_is_unauthorized() {
    ResponseEntity<String> response = loginError(new LoginInput("ghost", null, "StrongPass12!"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("Invalid credentials");
    assertThat(redis.keys("verification:*")).isEmpty();
    verify(eventProducer, never()).accept(any());
  }

  @Test
  void unverified_account_is_forbidden() {
    saveUser("jane_doe", "jane.doe@example.com", "StrongPass12!", false);

    ResponseEntity<String> response =
        loginError(new LoginInput(null, "jane.doe@example.com", "StrongPass12!"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Account has not been verified");
    assertThat(redis.keys("verification:*")).isEmpty();
    verify(eventProducer, never()).accept(any());
  }

  @Test
  void missing_username_and_email_is_unprocessable() {
    ResponseEntity<String> response = loginError(new LoginInput(null, null, "StrongPass12!"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("Username or email is required and cannot be blank");
  }

  @Test
  void blank_password_is_unprocessable() {
    ResponseEntity<String> response = loginError(new LoginInput("jane_doe", null, ""));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("password is required and cannot be blank");
  }

  @Test
  void invalid_email_format_is_unprocessable() {
    ResponseEntity<String> response =
        loginError(new LoginInput(null, "not-an-email", "StrongPass12!"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("Email not-an-email is not valid");
  }

  @Test
  void invalid_username_is_unprocessable() {
    ResponseEntity<String> response = loginError(new LoginInput("a", null, "StrongPass12!"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("Username a is invalid");
  }

  @Test
  void empty_json_body_is_unprocessable() {
    ResponseEntity<String> response = loginJson("{}");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("Username or email is required and cannot be blank");
  }

  @Test
  void missing_body_is_bad_request() {
    ResponseEntity<String> response = loginNoBody();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("Request body is missing or malformed.");
  }

  private JUser saveUser(String username, String email, String rawPassword, boolean verified) {
    return authRepository.save(
        JUser.builder()
            .username(username)
            .password(passwordEncoder.encode(rawPassword))
            .firstName("Jane")
            .lastName("Doe")
            .email(email)
            .verified(verified)
            .role(UserRole.CUSTOMER)
            .build());
  }

  private String singleVerificationToken() {
    Set<String> keys = redis.keys("verification:*");
    assertThat(keys).hasSize(1);
    return keys.iterator().next().substring("verification:".length());
  }

  private SendEmailRequested capturedEmailEvent() {
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<SendEmailRequested>> captor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(captor.capture());
    return captor.getValue().getFirst();
  }

  private ResponseEntity<MessageBody> login(LoginInput request) {
    return restTemplate.exchange(
        "/auth/login", HttpMethod.POST, new HttpEntity<>(request), MessageBody.class);
  }

  private ResponseEntity<String> loginError(LoginInput request) {
    return restTemplate.exchange(
        "/auth/login", HttpMethod.POST, new HttpEntity<>(request), String.class);
  }

  private ResponseEntity<String> loginJson(String jsonBody) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return restTemplate.exchange(
        "/auth/login", HttpMethod.POST, new HttpEntity<>(jsonBody, headers), String.class);
  }

  private ResponseEntity<String> loginNoBody() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return restTemplate.exchange(
        "/auth/login", HttpMethod.POST, new HttpEntity<>(headers), String.class);
  }
}
