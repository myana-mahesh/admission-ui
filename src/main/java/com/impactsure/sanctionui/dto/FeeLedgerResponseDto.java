package com.impactsure.sanctionui.dto;

import java.util.List;

import lombok.Data;

@Data
public class FeeLedgerResponseDto {
    private List<FeeLedgerRowDto> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private FeeLedgerSummaryDto summary;
}
