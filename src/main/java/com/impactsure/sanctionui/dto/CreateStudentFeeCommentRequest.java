package com.impactsure.sanctionui.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStudentFeeCommentRequest {

    private Long studentId;
    private String comment;
    private String commentedBy;
    private String commentType;
}
