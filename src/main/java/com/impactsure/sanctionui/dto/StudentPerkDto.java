package com.impactsure.sanctionui.dto;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class StudentPerkDto {
    private Long perkId;
    private String perkName;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
