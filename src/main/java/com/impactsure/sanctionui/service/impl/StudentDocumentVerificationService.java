package com.impactsure.sanctionui.service.impl;

import com.impactsure.sanctionui.entities.StudentDocumentVerification;
import com.impactsure.sanctionui.repository.StudentDocumentVerificationRepository;
import com.impactsure.sanctionui.repository.StudentDocumentVerificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudentDocumentVerificationService {

    @Autowired
    private StudentDocumentVerificationRepository repository;

    // Save Xerox / Original selection (by any user)
    public void saveReceivedType(Long admissionId,
                                 String documentCode,
                                 String receivedType) {

        StudentDocumentVerification entity =
                repository.findByAdmissionIdAndDocumentCode(admissionId, documentCode)
                        .orElseGet(() -> {
                            StudentDocumentVerification e = new StudentDocumentVerification();
                            e.setAdmissionId(admissionId);
                            e.setDocumentCode(documentCode);
                            return e;
                        });

        entity.setReceivedType(receivedType);
        repository.save(entity);
    }

    // HO verification
    public void verifyDocument(Long admissionId,
                               String documentCode,
                               String verifiedBy) {

        StudentDocumentVerification entity =
                repository.findByAdmissionIdAndDocumentCode(admissionId, documentCode)
                        .orElseThrow(() -> new RuntimeException("Document not found"));

        entity.setVerified(true);
        entity.setVerifiedBy(verifiedBy);
        entity.setVerifiedOn(LocalDateTime.now());

        repository.save(entity);
    }

    public Map<String, StudentDocumentVerification>
    getVerificationMap(Long admissionId) {

        List<StudentDocumentVerification> list =
                repository.findAll()
                        .stream()
                        .filter(v -> v.getAdmissionId().equals(admissionId))
                        .toList();

        Map<String, StudentDocumentVerification> map = new HashMap<>();
        for (StudentDocumentVerification v : list) {
            map.put(v.getDocumentCode(), v);
        }
        return map;
    }
}
