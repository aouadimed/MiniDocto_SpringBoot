package com.minidocto.availability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailableDoctorDTO {
    private String id; // Doctor ID
    private String name;
    private String category;
    private String image; // Static image URL for UI
    private String experience; // Placeholder string like "5+ years"
    private String datetime; // First available slot datetime as string
    private Double score; // Doctor rating/score
}
