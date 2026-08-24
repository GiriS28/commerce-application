package com.nisum.userservice.service;

import com.nisum.userservice.config.UserSortConfig;
import com.nisum.userservice.dto.PageResponse;
import com.nisum.userservice.dto.UserCreateRequest;
import com.nisum.userservice.dto.UserResponse;
import com.nisum.userservice.dto.UserUpdateRequest;
import com.nisum.userservice.entity.UserEntity;
import com.nisum.userservice.entity.UserStatus;
import com.nisum.userservice.exception.DuplicateUserException;
import com.nisum.userservice.exception.InvalidSortFieldException;
import com.nisum.userservice.exception.UserNotFoundException;
import com.nisum.userservice.mapper.UserMapper;
import com.nisum.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    private static final Logger log =
            LoggerFactory.getLogger(UserServiceImpl.class);

    public UserServiceImpl(UserRepository userRepository,
                           UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    public PageResponse<UserResponse> getAllUsers(
            String search,
            Pageable pageable) {

        validateSortFields(pageable);

        Page<UserEntity> users;

        if (search == null || search.isBlank()) {
            users = userRepository.findAll(pageable);
        } else {
            users = userRepository
                    .findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                            search,
                            search,
                            pageable
                    );
        }

        Page<UserResponse> page = users.map(userMapper::toResponse);

        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    private void validateSortFields(Pageable pageable) {

        pageable.getSort().forEach(order -> {

            String property = order.getProperty();

            if (!UserSortConfig.ALLOWED_SORT_FIELDS.contains(property)) {
                throw new InvalidSortFieldException(
                        "Sorting by field '" + property + "' is not allowed"
                );
            }
        });
    }

    @Override
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {

        log.info("Creating user with email={}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {

            log.warn("User creation failed. Email already exists: {}",
                    request.getEmail());

            throw new DuplicateUserException(
                    "A user with this email already exists"
            );
        }

        UserEntity user = new UserEntity();

        user.setId(UUID.randomUUID());
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setStatus(UserStatus.ACTIVE);

        UserEntity savedUser = userRepository.save(user);

        log.info("User created successfully with id={}",
                savedUser.getId());

        return userMapper.toResponse(savedUser);
    }

    @Override
    public UserResponse getUserById(UUID id) {

        log.info("Fetching user with id={}", id);

        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found with id={}", id);

                    return new UserNotFoundException(
                            "User not found with id: " + id
                    );
                });

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID id, UserUpdateRequest request) {

        log.info("Updating user with id={}", id);

        UserEntity user = userRepository.findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found with id: " + id
                        )
                );

        if (!user.getEmail().equalsIgnoreCase(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {

            log.warn(
                    "User update failed. Email already exists. userId={}, email={}",
                    id,
                    request.getEmail()
            );
            throw new DuplicateUserException(
                    "A user with this email already exists"
            );
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());

        UserEntity updatedUser = userRepository.save(user);

        log.info("User updated successfully with id={}", id);

        return userMapper.toResponse(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {

        log.info("Deleting user with id={}", id);
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found with id: " + id
                        )
                );
        log.info("User deleted successfully with id={}", id);

        userRepository.delete(user);
    }
}