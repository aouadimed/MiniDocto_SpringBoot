package com.minidocto.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResponse {
    private String id;
    private String name;
    private String email;
    private String role;
    private String specialty;
} 