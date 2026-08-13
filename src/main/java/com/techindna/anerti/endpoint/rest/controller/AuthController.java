package com.techindna.anerti.endpoint.rest.controller;

import com.techindna.anerti.dto.LoginInput;
import com.techindna.anerti.dto.MessageBody;
import com.techindna.anerti.dto.VerifyRegistrationResponse;
import com.techindna.anerti.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/login")
  public ResponseEntity<MessageBody> login(
      @RequestBody LoginInput request, HttpServletRequest servletRequest) {
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(authService.login(request, servletRequest));
  }

  @PostMapping("/resend-link")
  public ResponseEntity<MessageBody> resendVerificationLink(
      @RequestParam String email, HttpServletRequest servletRequest) {
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(authService.resendVerificationLink(email, servletRequest));
  }

  @GetMapping("/verification/{token}")
  public ResponseEntity<VerifyRegistrationResponse> verify(@PathVariable UUID token) {
    return ResponseEntity.status(HttpStatus.OK).body(authService.verify(token));
  }
}
