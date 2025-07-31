package com.minidocto.auth.dto;

import com.minidocto.user.model.Role;
import com.minidocto.availability.model.AvailabilitySlot;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class SignupRequest {
    @NotBlank(message = "Name is required")
    private String name;
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
    @NotNull(message = "Role is required")
    private Role role;
    private Integer score;
    private String specialty; // for PRO users only
    private List<String> appointments; // for USER only
} 