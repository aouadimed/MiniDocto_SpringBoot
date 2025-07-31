package com.minidocto.availability.dto;

import com.minidocto.availability.model.SlotStatus;
import lombok.*;
import org.bson.types.ObjectId;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotWithUserDTO {
    private String id;
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId doctorId;
    private String startTime;
    private String endTime;
    @Builder.Default
    private SlotStatus status = SlotStatus.AVAILABLE;
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId bookedBy;
    private String bookedByEmail; // User email who booked this slot
}
