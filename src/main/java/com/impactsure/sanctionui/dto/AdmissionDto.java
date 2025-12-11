package com.impactsure.sanctionui.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdmissionDto {

	private Long id;
    private String absId;             // e.g., ABS-000123
    private String studentName;
    private String mobile;
    private String courseName;
    private LocalDateTime createdAt;
    private String status;    
}
