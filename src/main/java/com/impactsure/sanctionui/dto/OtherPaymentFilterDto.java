package com.impactsure.sanctionui.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtherPaymentFilterDto {
    private Long fieldId;
    private String inputType;
    private String operator;
    private String value;
    private List<String> values;
}
