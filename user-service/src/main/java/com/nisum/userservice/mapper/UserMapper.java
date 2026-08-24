package com.nisum.userservice.mapper;

import com.nisum.userservice.dto.UserResponse;
import com.nisum.userservice.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(UserEntity entity) {

        return new UserResponse(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getCreatedAt(),
                entity.getStatus(),
                entity.getPhoneNumber()
        );
    }
}
