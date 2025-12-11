package com.impactsure.sanctionui.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.impactsure.sanctionui.entities.PaymentModeMaster;


public interface PaymentModeRepository extends JpaRepository<PaymentModeMaster, Long> {

    List<PaymentModeMaster> findByActiveTrueOrderByDisplayOrderAsc();

    PaymentModeMaster findByCodeAndActiveTrueOrderByDisplayOrderAsc(String mode);
}
