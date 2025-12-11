package com.impactsure.sanctionui.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.impactsure.sanctionui.entities.DocumentType;


@Repository
public interface DocumentTypeRepository extends JpaRepository<DocumentType, Long> {
	  Optional<DocumentType> findByCode(String code);

}
