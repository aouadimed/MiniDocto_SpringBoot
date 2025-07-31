package com.minidocto.appointment.dto;

import lombok.Data;

@Data
public class BookAppointmentRequest {
    private String doctorId;
    private String slotId;
    
    public boolean isValid() {
        return doctorId != null && !doctorId.trim().isEmpty() &&
               slotId != null && !slotId.trim().isEmpty();
    }
    
    public String getValidationError() {
        if (doctorId == null || doctorId.trim().isEmpty()) {
            return "Doctor ID is required";
        }
        if (slotId == null || slotId.trim().isEmpty()) {
            return "Slot ID is required";
        }
        return null;
    }
}
