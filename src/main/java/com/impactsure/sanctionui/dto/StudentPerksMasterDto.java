package com.impactsure.sanctionui.dto;

import java.time.OffsetDateTime;

import lombok.Data;

@Data
public class StudentPerksMasterDto {
    private Long id;
    private String name;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
