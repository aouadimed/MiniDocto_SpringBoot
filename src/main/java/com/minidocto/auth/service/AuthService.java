package com.minidocto.auth.service;

import com.minidocto.auth.dto.LoginRequest;
import com.minidocto.auth.dto.SignupRequest;
import com.minidocto.auth.dto.AuthResponse;
import com.minidocto.user.model.Role;
import com.minidocto.user.model.User;
import com.minidocto.user.repository.UserRepository;
import com.minidocto.shared.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use");
        }
        User.UserBuilder userBuilder = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole());
        if (request.getRole() == Role.PRO) {
            int randomScore = new Random().nextInt(101); // 0-100 inclusive
            userBuilder.score(randomScore)
                       .specialty(request.getSpecialty())
                       .appointments(null);
        } else if (request.getRole() == Role.USER) {
            userBuilder.score(null)
                       .specialty(null)
                       .appointments(request.getAppointments());
        }
        User user = userBuilder.build();
        userRepository.save(user);
        return new AuthResponse(null, null, user.getRole().name(), user.getEmail(), "User registered successfully");
    }

    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            String token = jwtUtil.generateToken(user.getEmail(), java.util.Map.of("role", user.getRole().name()));
            String refreshToken = jwtUtil.generateRefreshToken(user.getEmail(), java.util.Map.of("type", "refresh", "role", user.getRole().name()));
            if (user.getTokens() == null) user.setTokens(new java.util.ArrayList<>());
            user.getTokens().add(refreshToken);
            userRepository.save(user);
            return new AuthResponse(token, refreshToken, user.getRole().name(), user.getEmail(), "Login successful");
        } catch (AuthenticationException e) {
            throw new RuntimeException("Invalid credentials");
        }
    }

    public AuthResponse doctorLogin(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Validate user is a doctor (PRO role)
            if (user.getRole() != Role.PRO) {
                throw new RuntimeException("Access denied. This application is for healthcare professionals only.");
            }
            
            String token = jwtUtil.generateToken(user.getEmail(), java.util.Map.of("role", user.getRole().name(), "app", "DOCTOR_PORTAL"));
            String refreshToken = jwtUtil.generateRefreshToken(user.getEmail(), java.util.Map.of("type", "refresh", "role", user.getRole().name(), "app", "DOCTOR_PORTAL"));
            if (user.getTokens() == null) user.setTokens(new java.util.ArrayList<>());
            user.getTokens().add(refreshToken);
            userRepository.save(user);
            return new AuthResponse(token, refreshToken, user.getRole().name(), user.getEmail(), "Doctor login successful");
        } catch (AuthenticationException e) {
            throw new RuntimeException("Invalid doctor credentials");
        }
    }

    public AuthResponse patientLogin(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Validate user is a patient (USER role)
            if (user.getRole() != Role.USER) {
                throw new RuntimeException("Access denied. This application is for patients only.");
            }
            
            String token = jwtUtil.generateToken(user.getEmail(), java.util.Map.of("role", user.getRole().name(), "app", "PATIENT_PORTAL"));
            String refreshToken = jwtUtil.generateRefreshToken(user.getEmail(), java.util.Map.of("type", "refresh", "role", user.getRole().name(), "app", "PATIENT_PORTAL"));
            if (user.getTokens() == null) user.setTokens(new java.util.ArrayList<>());
            user.getTokens().add(refreshToken);
            userRepository.save(user);
            return new AuthResponse(token, refreshToken, user.getRole().name(), user.getEmail(), "Patient login successful");
        } catch (AuthenticationException e) {
            throw new RuntimeException("Invalid patient credentials");
        }
    }

    public AuthResponse refreshToken(String refreshToken) {
        String email = jwtUtil.extractUsername(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getTokens() == null || !user.getTokens().contains(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new RuntimeException("Refresh token expired or invalid");
        }
        String newToken = jwtUtil.generateToken(user.getEmail(), java.util.Map.of("role", user.getRole().name()));
        return new AuthResponse(newToken, refreshToken, user.getRole().name(), user.getEmail(), "Token refreshed");
    }

    public void logout(String email, String refreshToken) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getTokens() != null) {
            user.getTokens().remove(refreshToken);
            userRepository.save(user);
        }
    }
} 