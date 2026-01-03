package com.impactsure.sanctionui.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreateRequest {
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String password;
    private boolean temporaryPassword;
    private boolean enabled;
    private List<String> roleNames;
    private List<Long> branchIds;
}
