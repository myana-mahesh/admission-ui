package com.impactsure.sanctionui.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.impactsure.sanctionui.entities.Permission;
import com.impactsure.sanctionui.repository.PermissionRepository;
import com.impactsure.sanctionui.repository.RolePermissionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public List<Permission> listAll() {
        return permissionRepository.findAll();
    }

    public Permission create(Permission permission) {
        if (permission.getCode() == null || permission.getCode().isBlank()) {
            throw new IllegalArgumentException("Permission code is required.");
        }
        if (permissionRepository.existsByCode(permission.getCode().trim())) {
            throw new IllegalArgumentException("Permission code already exists.");
        }
        permission.setCode(permission.getCode().trim());
        return permissionRepository.save(permission);
    }

    public Permission update(Long id, Permission updated) {
        Permission existing = permissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found."));
        if (updated.getCode() != null && !updated.getCode().isBlank()) {
            String code = updated.getCode().trim();
            if (!code.equalsIgnoreCase(existing.getCode())
                    && permissionRepository.existsByCode(code)) {
                throw new IllegalArgumentException("Permission code already exists.");
            }
            existing.setCode(code);
        }
        existing.setLabel(updated.getLabel());
        existing.setResource(updated.getResource());
        existing.setAction(updated.getAction());
        existing.setDescription(updated.getDescription());
        return permissionRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        rolePermissionRepository.deleteByPermissionId(id);
        permissionRepository.deleteById(id);
    }
}
