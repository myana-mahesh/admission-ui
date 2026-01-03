package com.impactsure.sanctionui.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import com.impactsure.sanctionui.entities.RolePermission;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    List<RolePermission> findByRoleName(String roleName);
    List<RolePermission> findByRoleNameIn(List<String> roleNames);
    @Modifying
    @Transactional
    void deleteByRoleName(String roleName);

    @Modifying
    @Transactional
    void deleteByPermissionId(Long permissionId);
}
