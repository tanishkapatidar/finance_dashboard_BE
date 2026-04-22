package com.finance.dashboard.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6)
    private String password;

    @NotBlank
    @Size(max = 50)
    private String fullName;

    // Only VIEWER or ANALYST allowed on self-registration
    // Default to VIEWER if null
    private String role = "VIEWER";
}