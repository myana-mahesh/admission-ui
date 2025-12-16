// src/main/java/com/impactsure/sanctionui/dto/FeeInvoiceDto.java
package com.impactsure.sanctionui.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.Data;

@Data
public class FeeInvoiceDto {

    private Long invoiceId;
    private Long installmentId;   // <-- IMPORTANT for binding per row
    private String invoiceNumber;
    private BigDecimal amount;
    private String downloadUrl;
    private OffsetDateTime createdAt;
}
