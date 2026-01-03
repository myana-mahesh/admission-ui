package com.impactsure.sanctionui.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.impactsure.sanctionui.entities.BatchMaster;
import com.impactsure.sanctionui.repository.BatchMasterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BatchMasterService {

    private final BatchMasterRepository batchMasterRepository;

    public List<BatchMaster> getAllBatches() {
        return batchMasterRepository.findAll(Sort.by(Sort.Direction.ASC, "code"));
    }

    public Optional<BatchMaster> findById(Long id) {
        return batchMasterRepository.findById(id);
    }

    public BatchMaster save(BatchMaster batch) {
        if (batch.getCode() == null || batch.getCode().isBlank()) {
            throw new IllegalArgumentException("Batch code is required.");
        }

        if (batch.getBatchId() == null) {
            if (batchMasterRepository.existsByCode(batch.getCode())) {
                throw new IllegalArgumentException("Batch code already exists: " + batch.getCode());
            }
        } else {
            if (batchMasterRepository.existsByCodeAndBatchIdNot(batch.getCode(), batch.getBatchId())) {
                throw new IllegalArgumentException("Batch code already exists: " + batch.getCode());
            }
        }

        if (batch.getLabel() == null || batch.getLabel().isBlank()) {
            batch.setLabel(batch.getCode());
        }

        return batchMasterRepository.save(batch);
    }

    public void deleteById(Long id) {
        batchMasterRepository.deleteById(id);
    }
}
