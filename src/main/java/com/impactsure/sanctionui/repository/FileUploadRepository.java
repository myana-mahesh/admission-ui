package com.impactsure.sanctionui.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.impactsure.sanctionui.entities.FileUpload;


@Repository
public interface FileUploadRepository extends JpaRepository<FileUpload, Long> {
	  List<FileUpload> findByAdmissionAdmissionId(Long admissionId);
	  List<FileUpload> findByAdmissionAdmissionIdAndDocTypeDocTypeId(Long admissionId, Long docTypeId);
	List<FileUpload> findByAdmissionAdmissionIdAndInstallmentNotNull(Long admissionId);
	List<FileUpload> findByAdmissionAdmissionIdAndDocTypeCodeNot(Long admissionId, String string);

}
