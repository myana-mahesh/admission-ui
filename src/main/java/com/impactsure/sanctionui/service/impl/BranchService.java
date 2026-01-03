package com.impactsure.sanctionui.service.impl;

import com.impactsure.sanctionui.entities.BranchMaster;
import com.impactsure.sanctionui.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;

    public List<BranchMaster> getAllBranches() {
        return branchRepository.findAll();
    }

    public Optional<BranchMaster> findById(Long id) {
        return branchRepository.findById(id);
    }

    public BranchMaster save(BranchMaster branch) {
        if (branch.getCode() != null && !branch.getCode().isBlank()) {
            if (branch.getId() == null) {
                if (branchRepository.existsByCode(branch.getCode())) {
                    throw new IllegalArgumentException("Branch code already exists: " + branch.getCode());
                }
            } else {
                if (branchRepository.existsByCodeAndIdNot(branch.getCode(), branch.getId())) {
                    throw new IllegalArgumentException("Branch code already exists: " + branch.getCode());
                }
            }
        }
        return branchRepository.save(branch);
    }

    public void deleteById(Long id) {
        branchRepository.deleteById(id);
    }
}
