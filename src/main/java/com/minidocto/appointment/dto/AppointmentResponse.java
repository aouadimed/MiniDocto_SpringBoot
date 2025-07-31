package com.minidocto.appointment.dto;

import lombok.Builder;
import lombok.Data;
import com.minidocto.appointment.model.Appointment;

@Data
@Builder
public class AppointmentResponse {
    private boolean success;
    private String message;
    private AppointmentDTO appointment;
}
