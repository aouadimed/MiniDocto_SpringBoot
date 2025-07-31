package com.minidocto.appointment.dto;

import lombok.Builder;
import lombok.Data;
import com.minidocto.availability.dto.PaginationDTO;
import java.util.List;

@Data
@Builder
public class AppointmentListResponse {
    private boolean success;
    private String message;
    private List<AppointmentDTO> appointments;
    private int totalCount;
    private PaginationDTO pagination;
}
