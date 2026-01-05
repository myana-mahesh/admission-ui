package com.impactsure.sanctionui.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.impactsure.sanctionui.entities.UserBatchMapping;
import com.impactsure.sanctionui.repository.UserBatchMappingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserBatchMappingService {

    private final UserBatchMappingRepository userBatchMappingRepository;

    public List<Long> getBatchIds(String userId) {
        return userBatchMappingRepository.findByUserId(userId)
                .stream()
                .map(UserBatchMapping::getBatchId)
                .collect(Collectors.toList());
    }

    @Transactional
    public void replaceUserBatches(String userId, List<Long> batchIds) {
        userBatchMappingRepository.deleteByUserId(userId);
        userBatchMappingRepository.flush();
        if (batchIds == null || batchIds.isEmpty()) {
            return;
        }
        List<UserBatchMapping> mappings = batchIds.stream()
                .filter(batchId -> batchId != null)
                .distinct()
                .map(batchId -> UserBatchMapping.builder()
                        .userId(userId)
                        .batchId(batchId)
                        .build())
                .toList();
        userBatchMappingRepository.saveAll(mappings);
    }
}
