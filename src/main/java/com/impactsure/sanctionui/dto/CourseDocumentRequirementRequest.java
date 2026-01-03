package com.impactsure.sanctionui.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseDocumentRequirementRequest {
    private Long courseId;
    private Long docTypeId;
    private boolean optional;
}
