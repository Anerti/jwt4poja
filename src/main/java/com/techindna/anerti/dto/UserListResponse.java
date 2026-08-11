package com.techindna.anerti.dto;

import com.techindna.anerti.entity.User;
import java.util.List;

public record UserListResponse(List<User> data, Meta meta) {}
