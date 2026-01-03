package com.impactsure.sanctionui.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

@Data
public class FeeInstallmentPaymentDto {
    private Long paymentId;
    private String paymentMode;
    private String txnRef;
    private String receivedBy;
    private String status;
    private Boolean verified;
    private String verifiedBy;
    private java.time.LocalDateTime verifiedAt;
    private LocalDate paidOn;
    private BigDecimal amount;
    private String receiptUrl;
    private String receiptName;
    private String invoiceNumber;
    private String invoiceUrl;
}
