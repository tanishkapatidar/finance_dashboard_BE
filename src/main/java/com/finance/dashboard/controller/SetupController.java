package com.finance.dashboard.controller;

import com.finance.dashboard.dto.request.CreateUserRequest;
import com.finance.dashboard.dto.response.ApiResponse;
import com.finance.dashboard.dto.response.UserResponse;
import com.finance.dashboard.enums.Role;
import com.finance.dashboard.repository.UserRepository;
import com.finance.dashboard.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * One-time bootstrap endpoint.
 * Creates the first ADMIN user without requiring authentication.
 * Automatically disabled once any user exists in the database.
 */
@RestController
@RequestMapping("/api/setup")
@RequiredArgsConstructor
@Tag(name = "Setup", description = "One-time bootstrap — creates the first admin. Disabled after first user exists.")
public class SetupController {

    private final UserService userService;
    private final UserRepository userRepository;

    @PostMapping("/admin")
    @Operation(
        summary = "Create first admin (no auth required)",
        description = """
            Creates the initial admin user. This endpoint requires **no authentication** and
            is only available when the database has **zero users**.

            Once any user exists, this endpoint returns `409 Conflict` permanently.
            Use this once on first startup, then manage all subsequent users via `POST /api/users`.
            """
    )
    public ResponseEntity<ApiResponse<UserResponse>> createFirstAdmin(
            @Valid @RequestBody CreateUserRequest request) {

        if (userRepository.count() > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(
                    "Setup already complete. System already has users. " +
                    "Log in as an admin and use POST /api/users to create more users."));
        }

        // Force role to ADMIN regardless of what was sent
        request.setRole(Role.ADMIN);

        UserResponse created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(
                "First admin created successfully. You can now log in via POST /api/auth/login.", created));
    }
}
