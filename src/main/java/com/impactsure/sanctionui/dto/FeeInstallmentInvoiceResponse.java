package com.impactsure.sanctionui.dto;

import lombok.Data;

@Data
public class FeeInstallmentInvoiceResponse {
    private Long installmentId;
    private String invoiceNumber;
    private String downloadUrl;
    private String status;
}

