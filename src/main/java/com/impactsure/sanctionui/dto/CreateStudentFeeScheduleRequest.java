package com.impactsure.sanctionui.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStudentFeeScheduleRequest {

    private Long studentId;
    private LocalDate scheduledDate;
    private BigDecimal expectedAmount;
    private String scheduleType;
    private String notes;
    private String createdByUser;
}
