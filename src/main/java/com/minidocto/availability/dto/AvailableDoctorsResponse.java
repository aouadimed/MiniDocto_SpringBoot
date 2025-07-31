package com.minidocto.availability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailableDoctorsResponse {
    private List<AvailableDoctorDTO> availableDoctors;
    private int currentPage;
    private int totalPages;
}
