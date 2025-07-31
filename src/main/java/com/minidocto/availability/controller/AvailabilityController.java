package com.minidocto.availability.controller;

import com.minidocto.availability.dto.AvailabilityUpdateRequest;
import com.minidocto.availability.dto.AvailabilityResponse;
import com.minidocto.availability.dto.AvailableDoctorsResponse;
import com.minidocto.availability.dto.ScheduleGroupsResponse;
import com.minidocto.availability.model.AvailabilitySlot;
import com.minidocto.availability.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Controller for managing doctor availability (add/remove slots).
 */
@CrossOrigin(origins = "http://localhost:5173/")
@RestController
@RequestMapping("/availability")
@RequiredArgsConstructor
public class AvailabilityController {
    private final AvailabilityService availabilityService;

    @PostMapping("/update")
    public ResponseEntity<AvailabilityResponse> updateAvailability(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AvailabilityUpdateRequest request
    ) {
        String doctorId = userDetails.getUsername(); // Assuming username is user ID or use a service to fetch ID by email
        if (request.getAddedSlots() != null && !request.getAddedSlots().isEmpty()) {
            availabilityService.addSlots(doctorId, request.getAddedSlots());
        }
        if (request.getRemovedSlots() != null && !request.getRemovedSlots().isEmpty()) {
            availabilityService.removeSlots(doctorId, request.getRemovedSlots());
        }
        List<AvailabilitySlot> updatedSlots = availabilityService.getDoctorSlots(doctorId);
        return ResponseEntity.ok(new AvailabilityResponse(updatedSlots, "Availability updated successfully"));
    }

    @GetMapping("/my-slots")
    public ResponseEntity<List<AvailabilitySlot>> getMySlots(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "date", required = false) String date
    ) {
        String doctorId = userDetails.getUsername();
        if (date != null && !date.isEmpty()) {
            return ResponseEntity.ok(availabilityService.getDoctorSlotsByDate(doctorId, date));
        } else {
            return ResponseEntity.ok(availabilityService.getDoctorSlots(doctorId));
        }
    }

    @GetMapping("/available-doctors")
    public ResponseEntity<AvailableDoctorsResponse> getAvailableDoctors(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        AvailableDoctorsResponse response = availabilityService.getAvailableDoctors(page, size);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/doctors/{doctorId}/schedule-groups")
    public ResponseEntity<ScheduleGroupsResponse> getDoctorScheduleGroups(
            @PathVariable String doctorId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "30") int size,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String currentUserEmail = userDetails != null ? userDetails.getUsername() : null;
        ScheduleGroupsResponse response = availabilityService.getDoctorScheduleGroups(doctorId, page, size, currentUserEmail);
        return ResponseEntity.ok(response);
    }
} 