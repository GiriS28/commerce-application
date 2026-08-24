package com.nisum.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request used to create a new user")
public class UserCreateRequest {

    @Schema(
            description = "User's full name",
            example = "Rahul Kumar"
    )
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Schema(
            description = "User's email address",
            example = "rahul@example.com"
    )
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Schema(
            description = "User's phone number",
            example = "9876543210"
    )
    @Pattern(
            regexp = "^\\+?[0-9]{10,15}$",
            message = "Phone number must contain 10 to 15 digits"
    )
    private String phoneNumber;

    public UserCreateRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}