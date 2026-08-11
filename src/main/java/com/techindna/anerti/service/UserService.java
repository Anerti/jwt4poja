package com.techindna.anerti.service;

import com.techindna.anerti.dto.Meta;
import com.techindna.anerti.dto.UserListResponse;
import com.techindna.anerti.entity.enums.SortDirection;
import com.techindna.anerti.entity.enums.UserRole;
import com.techindna.anerti.mapper.UserMapper;
import com.techindna.anerti.repository.UserRepository;
import com.techindna.anerti.repository.model.JUser;
import com.techindna.anerti.validator.DataValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

  private static final int DEFAULT_PAGE = 1;
  private static final int DEFAULT_SIZE = 10;

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final DataValidator dataValidator;

  public UserListResponse listUsers(String search, int page, int size, SortDirection sort) {
    dataValidator.validateSearchString(search);

    int effectivePage = page < 1 ? DEFAULT_PAGE : page;
    int effectiveSize = size < 1 ? DEFAULT_SIZE : size;

    Page<JUser> jUsers =
        userRepository.searchUsers(
            UserRole.CUSTOMER,
            (search == null || search.isBlank()) ? null : search.strip(),
            PageRequest.of(
                effectivePage - 1,
                effectiveSize,
                Sort.by(
                    sort == SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC,
                    "createdAt")));

    return new UserListResponse(
        jUsers.getContent().stream().map(userMapper::toDomain).toList(),
        new Meta(effectivePage, effectiveSize, jUsers.getTotalElements()));
  }
}
