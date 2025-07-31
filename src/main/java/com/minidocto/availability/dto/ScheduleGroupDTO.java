package com.minidocto.availability.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ScheduleGroupDTO {
    private String id;
    private String date; // Single date instead of start/end date
    private int availableSlots;
    private List<SlotWithUserDTO> timeSlots;
    private boolean hasUserBookingInGroup; // Flag for user booking check
}
