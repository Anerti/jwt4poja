package com.techindna.anerti.service;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VerificationCodeStore {

  private static final Duration TTL = Duration.ofMinutes(15);
  private static final String KEY_PREFIX = "verification:";

  private final StringRedisTemplate redis;

  public void saveToken(String email, String token) {
    redis.opsForValue().set(KEY_PREFIX + token, email, TTL);
  }

  public Optional<String> getEmailByToken(String token) {
    return Optional.ofNullable(redis.opsForValue().get(KEY_PREFIX + token));
  }

  public void deleteByToken(String token) {
    redis.delete(KEY_PREFIX + token);
  }
}
