package com.impactsure.sanctionui.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import com.impactsure.sanctionui.entities.UserBranchMapping;

public interface UserBranchMappingRepository extends JpaRepository<UserBranchMapping, Long> {
    List<UserBranchMapping> findByUserId(String userId);
    @Modifying
    @Transactional
    void deleteByUserId(String userId);
}
