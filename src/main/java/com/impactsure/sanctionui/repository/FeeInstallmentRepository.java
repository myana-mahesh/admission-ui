package com.impactsure.sanctionui.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.impactsure.sanctionui.entities.FeeInstallment;


@Repository
public interface FeeInstallmentRepository extends JpaRepository<FeeInstallment, Long> {
	  List<FeeInstallment> findByAdmissionAdmissionIdOrderByStudyYearAscInstallmentNoAsc(Long admissionId);
	  Optional<FeeInstallment> findByAdmissionAdmissionIdAndStudyYearAndInstallmentNo(Long admissionId, Integer studyYear, Integer installmentNo);

}
