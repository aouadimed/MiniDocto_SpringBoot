package com.minidocto.availability.dto;

import com.minidocto.availability.model.AvailabilitySlot;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

/**
 * DTO for returning updated availability slots and a message.
 */
@Data
@AllArgsConstructor
public class AvailabilityResponse {
    private List<AvailabilitySlot> slots;
    private String message;
} 