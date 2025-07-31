package com.minidocto.appointment.dto;

import lombok.Builder;
import lombok.Data;
import com.minidocto.appointment.model.AppointmentStatus;

import java.time.LocalDateTime;

@Data
@Builder
public class AppointmentDTO {
    private String id;
    private String doctorId;
    private String patientId;
    private String slotId;
    private AppointmentStatus status;
    private String startTime;
    private String endTime;
    private String doctorName;
    private String doctorSpecialty;
    private String patientName;
    private String patientEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
