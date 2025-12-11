package com.impactsure.sanctionui.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.impactsure.sanctionui.entities.Address;


@Repository 
public interface AddressRepository extends JpaRepository<Address, Long> {}
