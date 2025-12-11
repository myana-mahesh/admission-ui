package com.impactsure.sanctionui.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.impactsure.sanctionui.entities.YearlyFees;

public interface YearlyFeesRepository extends JpaRepository<YearlyFees, Long>{

	List<YearlyFees> findByAdmissionAdmissionId(Long id);

}
