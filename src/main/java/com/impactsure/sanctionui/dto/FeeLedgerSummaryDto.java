package com.impactsure.sanctionui.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class FeeLedgerSummaryDto {
    private BigDecimal totalFeeAmount;
    private BigDecimal totalCollected;
    private BigDecimal totalPending;
    private BigDecimal overdueAmount;
    private BigDecimal dueNext7DaysAmount;
    private Long underVerificationCount;
}
