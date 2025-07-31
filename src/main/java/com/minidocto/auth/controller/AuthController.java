package com.minidocto.auth.controller;

import com.minidocto.auth.dto.SignupRequest;
import com.minidocto.auth.dto.LoginRequest;
import com.minidocto.auth.dto.RefreshRequest;
import com.minidocto.auth.dto.LogoutRequest;
import com.minidocto.auth.dto.AuthResponse;
import com.minidocto.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/doctor/login")
    public ResponseEntity<AuthResponse> doctorLogin(@Valid @RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.doctorLogin(request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(
                new AuthResponse(null, null, null, null, e.getMessage())
            );
        }
    }

    @PostMapping("/patient/login")
    public ResponseEntity<AuthResponse> patientLogin(@Valid @RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.patientLogin(request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(
                new AuthResponse(null, null, null, null, e.getMessage())
            );
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.getEmail(), request.getRefreshToken());
        return ResponseEntity.ok().build();
    }
} 