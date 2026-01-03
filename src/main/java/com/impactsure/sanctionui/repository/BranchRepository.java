package com.impactsure.sanctionui.repository;

import com.impactsure.sanctionui.entities.BranchMaster;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<BranchMaster, Long> {
    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);
}
