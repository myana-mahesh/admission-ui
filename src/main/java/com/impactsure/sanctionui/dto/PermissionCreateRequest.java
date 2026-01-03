package com.impactsure.sanctionui.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionCreateRequest {
    private String name;
    private String module;
    private boolean view;
    private boolean edit;
}
