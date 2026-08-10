package com.techindna.anerti.endpoint.rest.controller.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.dto.MessageBody;
import com.techindna.anerti.dto.RegisterInput;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;

@TestConstructor(autowireMode = AutowireMode.ALL)
class RegisterIT extends FacadeIT {

  private final TestRestTemplate restTemplate;
  private final AuthRepository authRepository;
  private final PasswordEncoder passwordEncoder;
  private final StringRedisTemplate redis;

  @MockBean private EventProducer<SendEmailRequested> eventProducer;

  RegisterIT(
      TestRestTemplate restTemplate,
      AuthRepository authRepository,
      PasswordEncoder passwordEncoder,
      StringRedisTemplate redis) {
    this.restTemplate = restTemplate;
    this.authRepository = authRepository;
    this.passwordEncoder = passwordEncoder;
    this.redis = redis;
  }

  @BeforeEach
  void clean() {
    authRepository.deleteAll();
    redis.keys("verification:*").forEach(redis::delete);
  }

  @Test
  void valid_registration_creates_pending_client_and_sends_verification_email() {
    ResponseEntity<MessageBody> response =
        register(
            new RegisterInput(
                "jane_doe",
                "StrongPass12!",
                "StrongPass12!",
                "Jane",
                "Doe",
                "jane.doe@example.com"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(response.getBody().message())
        .isEqualTo("An email has been sent to verify your account");

    JUser persisted = authRepository.findByEmail("jane.doe@example.com").orElseThrow();
    assertThat(persisted.getUsername()).isEqualTo("jane_doe");
    assertThat(persisted.getFirstName()).isEqualTo("Jane");
    assertThat(persisted.getLastName()).isEqualTo("Doe");
    assertThat(persisted.getVerified()).isFalse();
    assertThat(persisted.getRole()).isEqualTo(UserRole.CUSTOMER);
    assertThat(persisted.getPassword()).isNotEqualTo("StrongPass12!");
    assertThat(passwordEncoder.matches("StrongPass12!", persisted.getPassword())).isTrue();

    Set<String> keys = redis.keys("verification:*");
    assertThat(keys).hasSize(1);
    String token = keys.iterator().next().substring("verification:".length());
    assertThat(redis.opsForValue().get("verification:" + token)).isEqualTo("jane.doe@example.com");

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<SendEmailRequested>> captor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(captor.capture());
    SendEmailRequested event = captor.getValue().get(0);
    assertThat(event.getTo()).isEqualTo("jane.doe@example.com");
    assertThat(event.getSubject()).isEqualTo("Email Verification");
    assertThat(event.getHtmlBody()).contains("/auth/verification/" + token);
  }

  @Test
  void registration_normalizes_email_to_lowercase() {
    ResponseEntity<MessageBody> response =
        register(
            new RegisterInput(
                "jane_doe2",
                "StrongPass12!",
                "StrongPass12!",
                "Jane",
                "Doe",
                "Jane.Doe@Example.COM"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(authRepository.findByEmail("jane.doe@example.com")).isPresent();
    assertThat(authRepository.findByEmail("Jane.Doe@Example.COM")).isEmpty();
    assertThat(redis.keys("verification:*")).hasSize(1);
  }

  @Test
  void duplicate_email_returns_conflict() {
    saveUser("taken_user", "taken@example.com");

    ResponseEntity<String> response =
        registerError(
            new RegisterInput(
                "fresh_user",
                "StrongPass12!",
                "StrongPass12!",
                "Jane",
                "Doe",
                "taken@example.com"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).contains("You cannot use this email address");
    assertThat(redis.keys("verification:*")).isEmpty();
    verify(eventProducer, never()).accept(any());
  }

  @Test
  void duplicate_username_returns_conflict() {
    saveUser("taken_user", "taken@example.com");

    ResponseEntity<String> response =
        registerError(
            new RegisterInput(
                "taken_user",
                "StrongPass12!",
                "StrongPass12!",
                "Jane",
                "Doe",
                "fresh@example.com"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).contains("You cannot use this username");
    assertThat(redis.keys("verification:*")).isEmpty();
    verify(eventProducer, never()).accept(any());
  }

  @Test
  void blank_email_is_unprocessable() {
    ResponseEntity<String> response =
        registerError(
            new RegisterInput("jane_doe", "StrongPass12!", "StrongPass12!", "Jane", "Doe", ""));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("email is required and cannot be blank");
  }

  @Test
  void invalid_email_format_is_unprocessable() {
    ResponseEntity<String> response =
        registerError(
            new RegisterInput(
                "jane_doe", "StrongPass12!", "StrongPass12!", "Jane", "Doe", "not-an-email"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("Email not-an-email is not valid");
  }

  @Test
  void empty_json_body_is_unprocessable() {
    ResponseEntity<String> response = registerJson("{}");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("email is required and cannot be blank");
  }

  @Test
  void blank_password_is_unprocessable() {
    ResponseEntity<String> response =
        registerError(new RegisterInput("jane_doe", "", "", "Jane", "Doe", "jane.doe@example.com"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("password is required and cannot be blank");
  }

  @Test
  void short_password_is_unprocessable() {
    ResponseEntity<String> response =
        registerError(
            new RegisterInput(
                "jane_doe", "Short1!", "Short1!", "Jane", "Doe", "jane.doe@example.com"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("Password must be at least 12 characters");
  }

  @Test
  void password_without_uppercase_is_unprocessable() {
    ResponseEntity<String> response =
        registerError(
            new RegisterInput(
                "jane_doe",
                "lowercase123!",
                "lowercase123!",
                "Jane",
                "Doe",
                "jane.doe@example.com"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody())
        .contains("Password must contain at least one uppercase character");
  }

  @Test
  void password_without_digit_is_unprocessable() {
    ResponseEntity<String> response =
        registerError(
            new RegisterInput(
                "jane_doe", "Password!abc", "Password!abc", "Jane", "Doe", "jane.doe@example.com"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("Password must contain at least one digit");
  }

  @Test
  void password_without_special_character_is_unprocessable() {
    ResponseEntity<String> response =
        registerError(
            new RegisterInput(
                "jane_doe",
                "Password123abc",
                "Password123abc",
                "Jane",
                "Doe",
                "jane.doe@example.com"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("Password must contain at least one special character");
  }

  @Test
  void mismatched_passwords_are_unprocessable() {
    ResponseEntity<String> response =
        registerError(
            new RegisterInput(
                "jane_doe",
                "StrongPass12!",
                "Different12!",
                "Jane",
                "Doe",
                "jane.doe@example.com"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("Passwords do not match");
  }

  @Test
  void blank_confirm_password_is_unprocessable() {
    ResponseEntity<String> response =
        registerError(
            new RegisterInput(
                "jane_doe", "StrongPass12!", "", "Jane", "Doe", "jane.doe@example.com"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("confirmPassword is required and cannot be blank");
  }

  @Test
  void invalid_first_name_is_unprocessable() {
    ResponseEntity<String> response =
        registerError(
            new RegisterInput(
                "jane_doe",
                "StrongPass12!",
                "StrongPass12!",
                "john",
                "Doe",
                "jane.doe@example.com"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("firstName must start with a capital letter");
  }

  @Test
  void invalid_last_name_is_unprocessable() {
    ResponseEntity<String> response =
        registerError(
            new RegisterInput(
                "jane_doe",
                "StrongPass12!",
                "StrongPass12!",
                "Jane",
                "D0e",
                "jane.doe@example.com"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("lastName must start with a capital letter");
  }

  @Test
  void blank_username_is_unprocessable() {
    ResponseEntity<String> response =
        registerError(
            new RegisterInput(
                "", "StrongPass12!", "StrongPass12!", "Jane", "Doe", "jane.doe@example.com"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("username is required and cannot be blank");
  }

  @Test
  void invalid_username_is_unprocessable() {
    ResponseEntity<String> response =
        registerError(
            new RegisterInput(
                "a", "StrongPass12!", "StrongPass12!", "Jane", "Doe", "jane.doe@example.com"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("Username a is invalid");
  }

  @Test
  void missing_body_is_bad_request() {
    ResponseEntity<String> response = registerNoBody();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("Request body is missing or malformed.");
  }

  private JUser saveUser(String username, String email) {
    return authRepository.save(
        JUser.builder()
            .username(username)
            .password("encoded-placeholder")
            .firstName("John")
            .lastName("Doe")
            .email(email)
            .verified(true)
            .role(UserRole.CUSTOMER)
            .build());
  }

  private ResponseEntity<MessageBody> register(RegisterInput request) {
    return restTemplate.exchange(
        "/auth/register", HttpMethod.POST, new HttpEntity<>(request), MessageBody.class);
  }

  private ResponseEntity<String> registerError(RegisterInput request) {
    return restTemplate.exchange(
        "/auth/register", HttpMethod.POST, new HttpEntity<>(request), String.class);
  }

  private ResponseEntity<String> registerJson(String jsonBody) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return restTemplate.exchange(
        "/auth/register", HttpMethod.POST, new HttpEntity<>(jsonBody, headers), String.class);
  }

  private ResponseEntity<String> registerNoBody() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return restTemplate.exchange(
        "/auth/register", HttpMethod.POST, new HttpEntity<>(headers), String.class);
  }
}
