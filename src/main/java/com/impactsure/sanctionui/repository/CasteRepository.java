package com.impactsure.sanctionui.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.impactsure.sanctionui.entities.Caste;


@Repository
public interface CasteRepository extends JpaRepository<Caste, Long> { }
