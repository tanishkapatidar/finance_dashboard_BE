package com.finance.dashboard.service;

import com.finance.dashboard.dto.request.LoginRequest;
import com.finance.dashboard.dto.request.RegisterRequest;
import com.finance.dashboard.dto.response.ApiResponse;
import com.finance.dashboard.dto.response.AuthResponse;
import com.finance.dashboard.entity.User;
import com.finance.dashboard.exception.BadRequestException;
import com.finance.dashboard.repository.UserRepository;
import com.finance.dashboard.security.JwtUtil;
import com.finance.dashboard.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.finance.dashboard.exception.DuplicateResourceException;
import com.finance.dashboard.enums.Role;
import com.finance.dashboard.enums.UserStatus;


@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;      // ← ADD THIS
    private final PasswordEncoder passwordEncoder;

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtUtil.generateToken(principal);

        log.info("User '{}' logged in successfully", principal.getUsername());

        return AuthResponse.builder()
            .token(token)
            .tokenType("Bearer")
            .username(principal.getUsername())
            .role(principal.getUser().getRole().name())
            .userId(principal.getId())
            .build();
    }

    public ApiResponse<Void> register(RegisterRequest request) {
        // Prevent self-assigning ADMIN role
        if ("ADMIN".equalsIgnoreCase(request.getRole())) {
            throw new BadRequestException("Cannot self-register as ADMIN");
        }


        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole(Role.valueOf(request.getRole().toUpperCase()));
        user.setStatus(UserStatus.ACTIVE);

        userRepository.save(user);
        return ApiResponse.success("Registration successful", null);
    }
}
