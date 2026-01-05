package com.impactsure.sanctionui.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import com.impactsure.sanctionui.entities.UserBatchMapping;

public interface UserBatchMappingRepository extends JpaRepository<UserBatchMapping, Long> {
    List<UserBatchMapping> findByUserId(String userId);

    @Modifying
    @Transactional
    void deleteByUserId(String userId);
}
