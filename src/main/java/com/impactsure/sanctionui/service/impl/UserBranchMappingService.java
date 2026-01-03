package com.impactsure.sanctionui.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.impactsure.sanctionui.entities.UserBranchMapping;
import com.impactsure.sanctionui.repository.UserBranchMappingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserBranchMappingService {

    private final UserBranchMappingRepository userBranchMappingRepository;

    public List<Long> getBranchIds(String userId) {
        return userBranchMappingRepository.findByUserId(userId)
                .stream()
                .map(UserBranchMapping::getBranchId)
                .collect(Collectors.toList());
    }

    @Transactional
    public void replaceUserBranches(String userId, List<Long> branchIds) {
        userBranchMappingRepository.deleteByUserId(userId);
        userBranchMappingRepository.flush();
        if (branchIds == null || branchIds.isEmpty()) {
            return;
        }
        List<UserBranchMapping> mappings = branchIds.stream()
                .filter(branchId -> branchId != null)
                .distinct()
                .map(branchId -> UserBranchMapping.builder()
                        .userId(userId)
                        .branchId(branchId)
                        .build())
                .toList();
        userBranchMappingRepository.saveAll(mappings);
    }
}
