package com.nisum.userservice.dto;

import com.nisum.userservice.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "User information returned by the User Service")
public record UserResponse(

        @Schema(
                description = "Unique user identifier",
                example = "b1e0a70c-c274-4f1d-a824-abacc4f34f0a"
        )
        UUID id,

        @Schema(
                description = "User's full name",
                example = "Smruti Ranjan"
        )
        String name,

        @Schema(
                description = "User's email",
                example = "smruti@yop.com"
        )
        String email,

        @Schema(
                description = "Date and time when the user was created"
        )
        LocalDateTime createdAt,

        @Schema(
                description = "Current user status",
                example = "ACTIVE"
        )
        UserStatus status,

        @Schema(
                description = "User's phone number",
                example = "9876544321"
        )
        String phoneNumber
) {
}