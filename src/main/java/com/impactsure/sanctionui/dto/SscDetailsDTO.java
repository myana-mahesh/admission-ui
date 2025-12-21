package com.impactsure.sanctionui.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SscDetailsDTO {

    @NotNull
    @Min(0)
    @Max(100)
    private Double percentage;

    @NotBlank
    private String board;

    @NotNull
    private Integer passingYear;

    private String registrationNumber;
}
