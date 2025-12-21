package com.impactsure.sanctionui.service.impl;

import com.impactsure.sanctionui.entities.BranchMaster;
import com.impactsure.sanctionui.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;

    public List<BranchMaster> getAllBranches() {
        return branchRepository.findAll();
    }
}
