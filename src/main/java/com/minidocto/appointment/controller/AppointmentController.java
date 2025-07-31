package com.minidocto.appointment.controller;

import com.minidocto.appointment.dto.*;
import com.minidocto.appointment.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.minidocto.user.repository.UserRepository;
import com.minidocto.user.model.User;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentController.class);

    private final AppointmentService appointmentService;
    private final UserRepository userRepository;

    @PostMapping("/book")
    public ResponseEntity<AppointmentResponse> bookAppointment(
            @RequestBody BookAppointmentRequest request,
            Authentication authentication) {
        
        try {
            // Validate request
            if (!request.isValid()) {
                AppointmentResponse errorResponse = AppointmentResponse.builder()
                        .success(false)
                        .message(request.getValidationError())
                        .build();
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Get patient ID from authenticated user (JWT)
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userEmail = userDetails.getUsername();
            
            User patient = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            ObjectId patientId = new ObjectId(patient.getId());
            
            AppointmentResponse response = appointmentService.bookAppointment(
                    patientId, 
                    request.getDoctorId(), 
                    request.getSlotId()
            );
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error in booking appointment", e);
            AppointmentResponse errorResponse = AppointmentResponse.builder()
                    .success(false)
                    .message("Internal server error. Please try again.")
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<AppointmentListResponse> getMyAppointments(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Authentication authentication) {
        
        try {
            // Get patient ID from authenticated user
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userEmail = userDetails.getUsername();
            
            User patient = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            ObjectId patientId = new ObjectId(patient.getId());
            
            AppointmentListResponse response = appointmentService.getPatientAppointments(patientId, page, size);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving patient appointments", e);
            AppointmentListResponse errorResponse = AppointmentListResponse.builder()
                    .success(false)
                    .message("Failed to retrieve appointments")
                    .appointments(java.util.List.of())
                    .totalCount(0)
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/doctor/me")
    public ResponseEntity<AppointmentListResponse> getMyDoctorAppointments(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Authentication authentication) {
        
        try {
            // Get doctor ID from authenticated user
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userEmail = userDetails.getUsername();
            
            User doctor = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Verify user is a doctor (PRO role)
            if (!doctor.getRole().equals(com.minidocto.user.model.Role.PRO)) {
                AppointmentListResponse errorResponse = AppointmentListResponse.builder()
                        .success(false)
                        .message("Access denied. Only doctors can access this endpoint.")
                        .appointments(java.util.List.of())
                        .totalCount(0)
                        .build();
                return ResponseEntity.status(403).body(errorResponse);
            }
            
            ObjectId doctorId = new ObjectId(doctor.getId());
            
            AppointmentListResponse response = appointmentService.getDoctorAppointments(doctorId, page, size);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving doctor appointments", e);
            AppointmentListResponse errorResponse = AppointmentListResponse.builder()
                    .success(false)
                    .message("Failed to retrieve doctor appointments")
                    .appointments(java.util.List.of())
                    .totalCount(0)
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @DeleteMapping("/{appointmentId}")
    public ResponseEntity<AppointmentResponse> cancelAppointment(
            @PathVariable String appointmentId,
            Authentication authentication) {
        
        try {
            // Get patient ID from authenticated user
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userEmail = userDetails.getUsername();
            
            User patient = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            ObjectId patientId = new ObjectId(patient.getId());
            
            AppointmentResponse response = appointmentService.cancelAppointment(appointmentId, patientId);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error cancelling appointment", e);
            AppointmentResponse errorResponse = AppointmentResponse.builder()
                    .success(false)
                    .message("Internal server error. Please try again.")
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    // Debug endpoint to check slot status
    @GetMapping("/debug/slot/{slotId}")
    public ResponseEntity<?> debugSlot(@PathVariable String slotId) {
        try {
            return ResponseEntity.ok(appointmentService.debugSlot(slotId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
