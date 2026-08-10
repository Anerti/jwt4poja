package com.techindna.anerti.dto;

public record RegisterInput(
    String username,
    String password,
    String confirmPassword,
    String firstName,
    String lastName,
    String email) {}
