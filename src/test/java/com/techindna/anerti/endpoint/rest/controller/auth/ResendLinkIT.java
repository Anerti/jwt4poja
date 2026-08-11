package com.techindna.anerti.endpoint.rest.controller.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.techindna.anerti.conf.FacadeIT;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;

@TestConstructor(autowireMode = AutowireMode.ALL)
class ResendLinkIT extends FacadeIT {

  private final TestRestTemplate restTemplate;
  private final AuthRepository authRepository;
  private final StringRedisTemplate redis;

  @MockBean private EventProducer<SendEmailRequested> eventProducer;

  ResendLinkIT(
      TestRestTemplate restTemplate, AuthRepository authRepository, StringRedisTemplate redis) {
    this.restTemplate = restTemplate;
    this.authRepository = authRepository;
    this.redis = redis;
  }

  @BeforeEach
  void clean() {
    authRepository.deleteAll();
    redis.keys("verification:*").forEach(redis::delete);
  }

  @Test
  void pending_user_resend_sends_verification_email() {
    saveUser("jane_doe", "jane.doe@example.com", false);

    ResponseEntity<MessageBody> response = resend("jane.doe@example.com");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(response.getBody().message())
        .isEqualTo("A verification link has been sent to your email");

    String token = singleVerificationToken();
    assertThat(redis.opsForValue().get("verification:" + token)).isEqualTo("jane.doe@example.com");

    SendEmailRequested event = capturedEmailEvent();
    assertThat(event.getTo()).isEqualTo("jane.doe@example.com");
    assertThat(event.getSubject()).isEqualTo("Email Verification");
    assertThat(event.getHtmlBody()).contains("/auth/verification/" + token);
  }

  @Test
  void resend_link_normalizes_email_to_lowercase() {
    saveUser("jane_doe", "jane.doe@example.com", false);

    ResponseEntity<MessageBody> response = resend("Jane.Doe@Example.COM");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    String token = singleVerificationToken();
    assertThat(redis.opsForValue().get("verification:" + token)).isEqualTo("jane.doe@example.com");
    assertThat(capturedEmailEvent().getTo()).isEqualTo("jane.doe@example.com");
  }

  @Test
  void resend_link_issues_a_new_token() {
    saveUser("jane_doe", "jane.doe@example.com", false);

    resend("jane.doe@example.com");
    String firstToken = tokenFrom(capturedEmailEvent());
    clearInvocations(eventProducer);

    resend("jane.doe@example.com");
    String secondToken = tokenFrom(capturedEmailEvent());

    assertThat(secondToken).isNotEqualTo(firstToken);
    assertThat(redis.opsForValue().get("verification:" + secondToken))
        .isEqualTo("jane.doe@example.com");
  }

  @Test
  void verified_user_resend_is_forbidden() {
    saveUser("jane_doe", "jane.doe@example.com", true);

    ResponseEntity<String> response = resendError("jane.doe@example.com");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("No pending verification found for this email");
    assertThat(redis.keys("verification:*")).isEmpty();
    verify(eventProducer, never()).accept(any());
  }

  @Test
  void unknown_email_resend_is_forbidden() {
    ResponseEntity<String> response = resendError("ghost@example.com");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("No pending verification found for this email");
    assertThat(redis.keys("verification:*")).isEmpty();
    verify(eventProducer, never()).accept(any());
  }

  @Test
  void blank_email_is_unprocessable() {
    ResponseEntity<String> response = resendError("");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("email is required and cannot be blank");
  }

  @Test
  void invalid_email_format_is_unprocessable() {
    ResponseEntity<String> response = resendError("not-an-email");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).contains("Email not-an-email is not valid");
  }

  @Test
  void missing_email_param_is_bad_request() {
    ResponseEntity<String> response = resendNoParam();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("Missing or invalid request parameter: email");
  }

  private JUser saveUser(String username, String email, boolean verified) {
    return authRepository.save(
        JUser.builder()
            .username(username)
            .password("encoded-placeholder")
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

  private String tokenFrom(SendEmailRequested event) {
    String htmlBody = event.getHtmlBody();
    int start = htmlBody.indexOf("/auth/verification/") + "/auth/verification/".length();
    return htmlBody.substring(start, start + 36);
  }

  private SendEmailRequested capturedEmailEvent() {
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<SendEmailRequested>> captor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(captor.capture());
    return captor.getValue().getFirst();
  }

  private ResponseEntity<MessageBody> resend(String email) {
    return restTemplate.exchange(
        "/auth/resend-link?email=" + email, HttpMethod.POST, HttpEntity.EMPTY, MessageBody.class);
  }

  private ResponseEntity<String> resendError(String email) {
    return restTemplate.exchange(
        "/auth/resend-link?email=" + email, HttpMethod.POST, HttpEntity.EMPTY, String.class);
  }

  private ResponseEntity<String> resendNoParam() {
    return restTemplate.exchange(
        "/auth/resend-link", HttpMethod.POST, HttpEntity.EMPTY, String.class);
  }
}
