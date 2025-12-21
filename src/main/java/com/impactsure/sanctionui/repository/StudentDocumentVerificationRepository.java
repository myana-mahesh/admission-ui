package com.impactsure.sanctionui.repository;

import com.impactsure.sanctionui.entities.StudentDocumentVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentDocumentVerificationRepository extends JpaRepository<StudentDocumentVerification, Long> {

    Optional<StudentDocumentVerification>
    findByAdmissionIdAndDocumentCode(Long admissionId, String documentCode);
}
