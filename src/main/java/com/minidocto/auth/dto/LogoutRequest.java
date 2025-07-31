package com.minidocto.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class LogoutRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
} 