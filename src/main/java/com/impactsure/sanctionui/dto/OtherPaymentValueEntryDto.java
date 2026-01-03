package com.impactsure.sanctionui.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtherPaymentValueEntryDto {
    private Long optionId;
    private String value;
}
