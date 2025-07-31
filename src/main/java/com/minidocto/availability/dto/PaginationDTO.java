package com.minidocto.availability.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaginationDTO {
    private int currentPage;
    private int pageSize;
    private int totalPages;
    private int totalItems;
    private boolean hasNextPage;
    private boolean hasPreviousPage;
}
