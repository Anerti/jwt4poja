package com.techindna.anerti.service;

import com.techindna.anerti.dto.LoginInput;
import com.techindna.anerti.dto.MessageBody;
import com.techindna.anerti.dto.VerifyRegistrationResponse;
import com.techindna.anerti.endpoint.event.EventProducer;
import com.techindna.anerti.endpoint.event.model.SendEmailRequested;
import com.techindna.anerti.exception.http.UnauthorizedException;
import com.techindna.anerti.mapper.UserMapper;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.model.JUser;
import com.techindna.anerti.security.jwt.JwtTokenProvider;
import com.techindna.anerti.validator.UserValidator;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class AuthService {

  private static final DateTimeFormatter REGISTERED_AT_FORMAT =
      DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm 'UTC'").withZone(ZoneOffset.UTC);

  private final AuthRepository authRepository;
  private final UserMapper userMapper;
  private final UserValidator userValidator;
  private final PasswordEncoder passwordEncoder;
  private final VerificationCodeStore verificationCodeStore;
  private final TemplateEngine templateEngine;
  private final EventProducer<SendEmailRequested> eventProducer;
  private final JwtTokenProvider jwtTokenProvider;

  @Value("${app.base-url}")
  private String baseUrl;

  @Transactional
  public MessageBody login(LoginInput request, HttpServletRequest servletRequest) {
    userValidator.validateLogin(request);

    JUser jUser =
        request.email() != null && !request.email().isBlank()
            ? authRepository
                .findByEmail(request.email().strip().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"))
            : authRepository
                .findByUsername(request.username().strip())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

    if (!passwordEncoder.matches(request.password(), jUser.getPassword())) {
      throw new UnauthorizedException("Invalid credentials");
    }

    sendVerificationLink(
        jUser.getEmail(),
        jUser.getFirstName(),
        jUser.getLastName(),
        jUser.getUsername(),
        "Login Verification",
        "mail/login-verification",
        servletRequest);

    return new MessageBody("A verification link has been sent to your email");
  }

  @Transactional
  public VerifyRegistrationResponse verify(UUID token) {
    String tokenStr = token.toString();
    String email =
        verificationCodeStore
            .getEmailByToken(tokenStr)
            .orElseThrow(() -> new UnauthorizedException("Invalid verification token"));

    JUser jUser =
        authRepository
            .findByEmail(email)
            .orElseThrow(() -> new UnauthorizedException("Invalid verification token"));

    verificationCodeStore.deleteByToken(tokenStr);

    String jwtToken =
        jwtTokenProvider.generateToken(jUser.getId().toString(), jUser.getRole().name());
    return new VerifyRegistrationResponse(jwtToken, userMapper.toDomain(jUser));
  }

  private void sendVerificationLink(
      String email,
      String firstName,
      String lastName,
      String username,
      String subject,
      String template,
      HttpServletRequest servletRequest) {
    String token = UUID.randomUUID().toString();
    verificationCodeStore.saveToken(email, token);

    String verificationUrl = String.format("%s/auth/verification/%s", baseUrl, token);
    Context context = new Context();
    context.setVariables(
        Map.of(
            "verificationUrl", verificationUrl,
            "firstName", firstName,
            "lastName", lastName,
            "username", username,
            "email", email,
            "clientIp", extractClientIp(servletRequest),
            "userAgent", userAgent(servletRequest),
            "time", REGISTERED_AT_FORMAT.format(Instant.now())));
    String htmlBody = templateEngine.process(template, context);

    eventProducer.accept(
        List.of(
            SendEmailRequested.builder().to(email).subject(subject).htmlBody(htmlBody).build()));
  }

  private String extractClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    return (xForwardedFor != null && !xForwardedFor.isBlank())
        ? xForwardedFor.split(",")[0].trim()
        : request.getRemoteAddr();
  }

  private String userAgent(HttpServletRequest request) {
    String userAgent = request.getHeader("User-Agent");
    return userAgent != null ? userAgent : "Unknown";
  }
}
