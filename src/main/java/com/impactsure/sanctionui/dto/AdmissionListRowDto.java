package com.impactsure.sanctionui.dto;

import java.time.OffsetDateTime;

import lombok.Data;

@Data
public class AdmissionListRowDto {
    private Long admissionId;
    private String absId;
    private String studentName;
    private String studentMobile;
    private String courseName;
    private OffsetDateTime createdAt;
    private String status;
    private Boolean branchApproved;
}
