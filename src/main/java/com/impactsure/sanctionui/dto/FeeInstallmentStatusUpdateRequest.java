package com.impactsure.sanctionui.dto;

import lombok.Data;

@Data
public class FeeInstallmentStatusUpdateRequest {
    private String status;   // e.g. "Paid" or "Un Paid"

    public FeeInstallmentStatusUpdateRequest() {}

    public FeeInstallmentStatusUpdateRequest(String status) {
        this.status = status;
    }
}
