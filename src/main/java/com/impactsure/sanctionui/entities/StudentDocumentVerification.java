package com.impactsure.sanctionui.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_document_verification",
        uniqueConstraints = @UniqueConstraint(columnNames = {"admission_id", "document_code"}))
@Data
public class StudentDocumentVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admission_id", nullable = false)
    private Long admissionId;

    @Column(name = "document_code", nullable = false)
    private String documentCode; // SSC10, HSC12, MIG etc

    @Column(name = "received_type")
    private String receivedType; // XEROX / ORIGINAL

    @Column(name = "verified")
    private Boolean verified = false;

    @Column(name = "verified_by")
    private String verifiedBy;

    @Column(name = "verified_on")
    private LocalDateTime verifiedOn;

    // getters & setters
}

