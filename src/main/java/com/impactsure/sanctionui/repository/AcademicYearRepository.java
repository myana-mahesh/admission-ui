package com.impactsure.sanctionui.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.impactsure.sanctionui.entities.AcademicYear;


@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {
	  Optional<AcademicYear> findByLabel(String label);

}
