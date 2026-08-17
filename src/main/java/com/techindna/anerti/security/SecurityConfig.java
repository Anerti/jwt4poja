package com.techindna.anerti.security;

import com.techindna.anerti.exception.ErrorBody;
import com.techindna.anerti.security.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, JwtAuthenticationFilter jwtAuthFilter) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/auth/**", "/ping")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/courses")
                    .permitAll()
                    .requestMatchers("/health/email", "/health/bucket")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/teachers")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/teachers")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/courses")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/students")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/students")
                    .hasAnyRole("ADMIN", "TEACHER")
                    .requestMatchers(HttpMethod.POST, "/groups")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/groups")
                    .hasAnyRole("ADMIN", "TEACHER")
                    .requestMatchers(HttpMethod.DELETE, "/groups/{groupId}")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/teacher-courses")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/exams")
                    .hasAnyRole("ADMIN", "TEACHER")
                    .requestMatchers(HttpMethod.POST, "/grades")
                    .hasAnyRole("ADMIN", "TEACHER")
                    .requestMatchers(HttpMethod.GET, "/grades/**")
                    .hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                    .requestMatchers(HttpMethod.GET, "/exams")
                    .hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(
                        (request, response, authException) ->
                            ErrorBody.send(
                                response, HttpStatus.UNAUTHORIZED, "Authentication required."))
                    .accessDeniedHandler(
                        (request, response, accessDeniedException) ->
                            ErrorBody.send(
                                response, HttpStatus.FORBIDDEN, "Insufficient privileges.")));

    return http.build();
  }
}
