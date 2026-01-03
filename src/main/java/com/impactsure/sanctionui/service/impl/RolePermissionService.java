package com.impactsure.sanctionui.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.impactsure.sanctionui.entities.Permission;
import com.impactsure.sanctionui.entities.RolePermission;
import com.impactsure.sanctionui.repository.PermissionRepository;
import com.impactsure.sanctionui.repository.RolePermissionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;

    public List<RolePermission> listByRole(String roleName) {
        return rolePermissionRepository.findByRoleName(roleName);
    }

    @Transactional
    public void replaceRolePermissions(String roleName, List<Long> permissionIds) {
        String normalizedRole = roleName == null ? null : roleName.trim();
        rolePermissionRepository.deleteByRoleName(normalizedRole);
        rolePermissionRepository.flush();
        if (permissionIds == null || permissionIds.isEmpty()) {
            return;
        }
        Map<Long, Permission> permissions = permissionRepository.findAllById(permissionIds)
                .stream()
                .collect(Collectors.toMap(Permission::getId, p -> p));
        List<RolePermission> mappings = permissionIds.stream()
                .distinct()
                .map(id -> permissions.get(id))
                .filter(p -> p != null)
                .map(p -> RolePermission.builder()
                        .roleName(normalizedRole)
                        .permission(p)
                        .build())
                .toList();
        rolePermissionRepository.saveAll(mappings);
    }
}
