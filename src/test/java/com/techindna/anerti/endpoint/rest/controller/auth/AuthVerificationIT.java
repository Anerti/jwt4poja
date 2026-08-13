package com.techindna.anerti.endpoint.rest.controller.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.techindna.anerti.conf.FacadeIT;
import com.techindna.anerti.dto.VerifyRegistrationResponse;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.enums.UserRole;
import com.techindna.anerti.repository.model.JUser;
import com.techindna.anerti.security.jwt.JwtTokenProvider;
import com.techindna.anerti.service.VerificationCodeStore;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;

@TestConstructor(autowireMode = AutowireMode.ALL)
class AuthVerificationIT extends FacadeIT {

  private final TestRestTemplate restTemplate;
  private final AuthRepository authRepository;
  private final VerificationCodeStore verificationCodeStore;
  private final JwtTokenProvider jwtTokenProvider;
  private final StringRedisTemplate redis;

  AuthVerificationIT(
      TestRestTemplate restTemplate,
      AuthRepository authRepository,
      VerificationCodeStore verificationCodeStore,
      JwtTokenProvider jwtTokenProvider,
      StringRedisTemplate redis) {
    this.restTemplate = restTemplate;
    this.authRepository = authRepository;
    this.verificationCodeStore = verificationCodeStore;
    this.jwtTokenProvider = jwtTokenProvider;
    this.redis = redis;
  }

  @BeforeEach
  void clean() {
    authRepository.deleteAll();
  }

  @Test
  void valid_token_verifies_user_and_returns_jwt() {
    JUser user = saveUser("jdoe", UserRole.ADMIN, false);
    String token = UUID.randomUUID().toString();
    verificationCodeStore.saveToken(user.getEmail(), token);

    assertThat(authRepository.findById(user.getId())).isPresent();
    assertThat(verificationCodeStore.getEmailByToken(token)).contains(user.getEmail());

    ResponseEntity<VerifyRegistrationResponse> response = verify(UUID.fromString(token));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().token()).isNotBlank();
    assertThat(response.getBody().user().id()).isEqualTo(user.getId());
    assertThat(response.getBody().user().username()).isEqualTo("jdoe");
    assertThat(response.getBody().user().role()).isEqualTo(UserRole.ADMIN);

    assertThat(authRepository.findById(user.getId()).orElseThrow().getVerified()).isTrue();
    assertThat(verificationCodeStore.getEmailByToken(token)).isEmpty();

    Claims claims = jwtTokenProvider.validateToken(response.getBody().token());
    assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
    assertThat(claims.get("role")).isEqualTo(UserRole.ADMIN.name());
  }

  @Test
  void already_verified_user_still_gets_200() {
    JUser user = saveUser("jdoe", UserRole.ADMIN, true);
    String token = UUID.randomUUID().toString();
    verificationCodeStore.saveToken(user.getEmail(), token);

    ResponseEntity<VerifyRegistrationResponse> response = verify(UUID.fromString(token));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(authRepository.findById(user.getId()).orElseThrow().getVerified()).isTrue();
    assertThat(verificationCodeStore.getEmailByToken(token)).isEmpty();
  }

  @Test
  void unknown_token_is_unauthorized() {
    saveUser("jdoe", UserRole.ADMIN, false);
    UUID unknownToken = UUID.randomUUID();

    assertThat(verificationCodeStore.getEmailByToken(unknownToken.toString())).isEmpty();

    ResponseEntity<String> response = verifyError(unknownToken.toString());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("Invalid verification token");
  }

  @Test
  void token_without_matching_user_is_unauthorized() {
    String token = UUID.randomUUID().toString();
    verificationCodeStore.saveToken("ghost@example.com", token);

    assertThat(verificationCodeStore.getEmailByToken(token)).contains("ghost@example.com");

    ResponseEntity<String> response = verifyError(token);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("Invalid verification token");
  }

  @Test
  void used_token_is_unauthorized() {
    JUser user = saveUser("jdoe", UserRole.ADMIN, false);
    String token = UUID.randomUUID().toString();
    verificationCodeStore.saveToken(user.getEmail(), token);

    assertThat(verify(UUID.fromString(token)).getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<String> response = verifyError(token);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("Invalid verification token");
  }

  @Test
  void expired_token_is_unauthorized() throws InterruptedException {
    JUser user = saveUser("jdoe", UserRole.ADMIN, false);
    String token = UUID.randomUUID().toString();
    redis.opsForValue().set("verification:" + token, user.getEmail(), Duration.ofSeconds(1));

    assertThat(verificationCodeStore.getEmailByToken(token)).contains(user.getEmail());

    Thread.sleep(1500);

    ResponseEntity<String> response = verifyError(token);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("Invalid verification token");
  }

  @Test
  void malformed_token_is_bad_request() {
    ResponseEntity<String> response = verifyError("not-a-uuid");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("Invalid parameter: token");
  }

  private JUser saveUser(String username, UserRole role, boolean verified) {
    return authRepository.save(
        JUser.builder()
            .username(username)
            .password("encoded-placeholder")
            .firstName("John")
            .lastName("Doe")
            .email(username + "@example.com")
            .verified(verified)
            .role(role)
            .build());
  }

  private ResponseEntity<VerifyRegistrationResponse> verify(UUID token) {
    return restTemplate.exchange(
        "/auth/verification/{token}",
        HttpMethod.GET,
        HttpEntity.EMPTY,
        VerifyRegistrationResponse.class,
        token);
  }

  private ResponseEntity<String> verifyError(String token) {
    return restTemplate.exchange(
        "/auth/verification/{token}", HttpMethod.GET, HttpEntity.EMPTY, String.class, token);
  }
}
