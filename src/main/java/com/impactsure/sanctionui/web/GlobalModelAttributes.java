package com.impactsure.sanctionui.web;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.impactsure.sanctionui.entities.RolePermission;
import com.impactsure.sanctionui.repository.RolePermissionRepository;

@ControllerAdvice
public class GlobalModelAttributes {

    private final RolePermissionRepository rolePermissionRepository;

    public GlobalModelAttributes(RolePermissionRepository rolePermissionRepository) {
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @ModelAttribute("userName")
    public String populateUserName(@AuthenticationPrincipal OidcUser oidcUser) {
        if (oidcUser == null) {
            return null;
        }
        String name = oidcUser.getFullName();
        if (name == null || name.isBlank()) {
            name = oidcUser.getPreferredUsername();
        }
        return name;
    }

    @ModelAttribute("role")
    public String populateRole(@AuthenticationPrincipal OidcUser oidcUser) {
        if (oidcUser == null) {
            return null;
        }
        List<String> roles = extractRoles(oidcUser);
        if (roles.contains("SUPER_ADMIN")) {
            return "SUPER_ADMIN";
        }
        if (roles.contains("ADMIN")) {
            return "ADMIN";
        }
        if (roles.contains("HO")) {
            return "HO";
        }
        if (roles.contains("BRANCH_USER")) {
            return "BRANCH_USER";
        }
        return roles.isEmpty() ? null : roles.get(0);
    }

    @ModelAttribute("userRoleLabel")
    public String populateUserRoleLabel(@ModelAttribute("role") String role) {
        if (role == null) {
            return null;
        }
        return switch (role) {
            case "SUPER_ADMIN" -> "Super Admin";
            case "ADMIN" -> "Administrator";
            case "BRANCH_USER" -> "Branch User";
            case "HO" -> "Head Office";
            default -> role;
        };
    }

    @ModelAttribute("isSuperAdmin")
    public boolean populateIsSuperAdmin(@AuthenticationPrincipal OidcUser oidcUser) {
        if (oidcUser == null) {
            return false;
        }
        return extractRoles(oidcUser).contains("SUPER_ADMIN");
    }

    @ModelAttribute("permissionSet")
    public Set<String> populatePermissionSet(@AuthenticationPrincipal OidcUser oidcUser) {
        if (oidcUser == null) {
            return Collections.emptySet();
        }
        List<String> roles = extractRoles(oidcUser);
        if (roles.contains("SUPER_ADMIN") || roles.isEmpty()) {
            return Collections.emptySet();
        }
        List<RolePermission> mappings = rolePermissionRepository.findByRoleNameIn(roles);
        return mappings.stream()
                .map(RolePermission::getPermission)
                .filter(p -> p != null && p.getResource() != null && p.getAction() != null)
                .map(p -> p.getResource() + ":" + p.getAction().toUpperCase())
                .collect(Collectors.toSet());
    }

    @ModelAttribute("permissionResources")
    public Set<String> populatePermissionResources(@AuthenticationPrincipal OidcUser oidcUser) {
        if (oidcUser == null) {
            return Collections.emptySet();
        }
        List<String> roles = extractRoles(oidcUser);
        if (roles.contains("SUPER_ADMIN") || roles.isEmpty()) {
            return Collections.emptySet();
        }
        List<RolePermission> mappings = rolePermissionRepository.findByRoleNameIn(roles);
        return mappings.stream()
                .map(RolePermission::getPermission)
                .filter(p -> p != null && p.getResource() != null)
                .map(p -> p.getResource())
                .collect(Collectors.toSet());
    }

    private List<String> extractRoles(OidcUser oidcUser) {
        return oidcUser.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .toList();
    }
}
