package com.impactsure.sanctionui.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.impactsure.sanctionui.entities.StudentAddress;


@Repository 
public interface StudentAddressRepository extends JpaRepository<StudentAddress, Long> {
	  Optional<StudentAddress> findByStudentStudentIdAndType(Long studentId, String type);
	}
