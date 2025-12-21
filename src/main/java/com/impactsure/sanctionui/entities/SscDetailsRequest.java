package com.impactsure.sanctionui.entities;

import lombok.Data;

@Data
public class SscDetailsRequest {
    private Double percentage;
    private String board;
    private Integer passingYear;
    private String registrationNumber;
}
