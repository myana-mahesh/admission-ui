package com.impactsure.sanctionui.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.impactsure.sanctionui.entities.BatchMaster;

public interface BatchMasterRepository extends JpaRepository<BatchMaster, Long> {
    boolean existsByCode(String code);

    boolean existsByCodeAndBatchIdNot(String code, Long batchId);
}
