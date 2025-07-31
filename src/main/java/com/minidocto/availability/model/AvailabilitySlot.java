package com.minidocto.availability.model;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "availability_slots")
public class AvailabilitySlot {
    @Id
    private String id;
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId doctorId;
    private String startTime;
    private String endTime;
    @Builder.Default
    private SlotStatus status = SlotStatus.AVAILABLE;
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId bookedBy;
} 