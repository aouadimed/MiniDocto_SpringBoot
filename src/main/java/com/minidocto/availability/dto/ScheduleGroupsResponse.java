package com.minidocto.availability.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ScheduleGroupsResponse {
    private boolean success;
    private String message;
    private ScheduleGroupsData data;
    
    @Data
    @Builder
    public static class ScheduleGroupsData {
        private List<ScheduleGroupDTO> scheduleGroups;
        private PaginationDTO pagination;
    }
}
