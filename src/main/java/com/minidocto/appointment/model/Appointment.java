package com.minidocto.appointment.model;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "appointments")
public class Appointment {
    @Id
    private String id;
    
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId doctorId;
    
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId patientId;
    
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId slotId; // Reference to availability slot
    
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.PENDING;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
