package com.nisum.userservice.service;

import com.nisum.userservice.dto.PageResponse;
import com.nisum.userservice.dto.UserCreateRequest;
import com.nisum.userservice.dto.UserResponse;
import com.nisum.userservice.dto.UserUpdateRequest;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface UserService {

    PageResponse<UserResponse> getAllUsers(String search, Pageable pageable);

    UserResponse getUserById(UUID id);

    UserResponse createUser(UserCreateRequest request);

    UserResponse updateUser(UUID id, UserUpdateRequest request);

    void deleteUser(UUID id);
}
