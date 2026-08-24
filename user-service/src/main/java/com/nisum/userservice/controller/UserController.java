package com.nisum.userservice.controller;

import com.nisum.userservice.dto.PageResponse;
import com.nisum.userservice.dto.UserCreateRequest;
import com.nisum.userservice.dto.UserResponse;
import com.nisum.userservice.dto.UserUpdateRequest;
import com.nisum.userservice.service.UserService;
import com.nisum.userservice.service.UserServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@Tag(
        name = "Users",
        description = "APIs for managing users"
)
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserServiceImpl userService) {
        this.userService = userService;
    }

    @Operation(
            summary = "Get users",
            description = "Returns a paginated list of users with optional search and sorting"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Users successfully retrieved"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid pagination or sorting parameters"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error"
            )
    })
    @GetMapping
    public PageResponse<UserResponse> getAllUsers(
            @RequestParam(required = false) String search,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        return userService.getAllUsers(search, pageable);
    }

    @Operation(
            summary = "Create user",
            description = "Creates a new user"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "User successfully created"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "User with the email already exists"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error"
            )
    })
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody UserCreateRequest request) {

        UserResponse response = userService.createUser(request);

        URI location = URI.create(
                "/api/v1/users/" + response.id()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @Operation(
            summary = "Get user by ID",
            description = "Returns a single user using the user UUID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User found"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error"
            )
    })
    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable UUID id) {
        return userService.getUserById(id);
    }

    @Operation(
            summary = "Update user",
            description = "Updates an existing user"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User successfully updated"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email already belongs to another user"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error"
            )
    })
    @PutMapping("/{id}")
    public UserResponse updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequest request) {

        return userService.updateUser(id, request);
    }

    @Operation(
            summary = "Delete user",
            description = "Deletes an existing user"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "User successfully deleted"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error"
            )
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
    }
}