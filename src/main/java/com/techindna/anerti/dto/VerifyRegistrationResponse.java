package com.techindna.anerti.dto;

import com.techindna.anerti.entity.User;

public record VerifyRegistrationResponse(String token, User user) {}
