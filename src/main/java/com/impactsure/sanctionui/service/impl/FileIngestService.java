package com.impactsure.sanctionui.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.impactsure.sanctionui.dto.InstallmentUpsertRequest;
import com.impactsure.sanctionui.dto.MultipleUploadRequest;
import com.impactsure.sanctionui.dto.UploadRequest;
import com.impactsure.sanctionui.entities.FeeInstallment;
import com.impactsure.sanctionui.entities.FileUpload;
import com.impactsure.sanctionui.utils.HashUtil;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileIngestService {

private final FileStorageService storage;
private final RestTemplate restTemplate;

@Value("${admission.service.url}") // e.g. https://api.example.com/admissions/{id}/uploads
private String admissionApiUrlTemplate;

public List<FileUpload> ingestAndForward(Long id, MultipleUploadRequest metadata,
		List<MultipartFile> files, 
		String bearerToken,String role) {
 try {
   if (metadata!=null && files!=null && metadata.getFiles()!=null && metadata.getFiles().size() != files.size()) {
     throw new IllegalArgumentException("metadata.files count must match files count");
   }

   // 1) Store each file + compute sha256
   List<UploadRequest> finalFiles = new ArrayList<>();
   List<InstallmentUpsertRequest> items =metadata.getInstallments();
   if(files!=null) {
	   for (int i = 0; i < files.size(); i++) {
		     MultipartFile mf = files.get(i);
		     UploadRequest meta = metadata.getFiles().get(i);

		     // Store
		     var stored = storage.store(id, mf);

		     // Hash (from stored file on disk)
		     String sha256;
		     try (InputStream in = Files.newInputStream(stored.path())) {
		       sha256 = HashUtil.sha256Hex(in);
		     }

		     // Build final DTO
		     finalFiles.add(UploadRequest.builder()
		         .docTypeCode(meta.getDocTypeCode())
		         .filename(meta.getFilename() != null ? meta.getFilename() : mf.getOriginalFilename())
		         .mimeType(meta.getMimeType() != null ? meta.getMimeType() : mf.getContentType())
		         .sizeBytes(meta.getSizeBytes() != null ? meta.getSizeBytes() : Math.toIntExact(mf.getSize()))
		         .storageUrl(stored.url()) // <-- URL that your app serves
		         .sha256(sha256)
		         .label(meta.getLabel())
		         .installmentTempId(meta.getInstallmentTempId())
		         .build()
		     );
		     

		   }
   }
   

   // 2) Forward to Admission API (JSON)
   var finalPayload = new MultipleUploadRequest(finalFiles,metadata.getInstallments());
   HttpHeaders headers = new HttpHeaders();
   headers.setContentType(MediaType.APPLICATION_JSON);
   headers.setBearerAuth(bearerToken);
   
   	
   String installmentUrl = admissionApiUrlTemplate +"/api/admissions/"+id+"/installments/bulk?role="+role;
   
   ParameterizedTypeReference<List<FeeInstallment>> type =
		      new ParameterizedTypeReference<>() {};
		      
   ResponseEntity<List<FeeInstallment>> installmentReposne = restTemplate.exchange(
		   installmentUrl, HttpMethod.POST, new HttpEntity<>(metadata.getInstallments(), headers), type
   );
   

   if(installmentReposne.getStatusCode().is2xxSuccessful()) {
	   List<FeeInstallment> installments =installmentReposne.getBody();
	   int i=0;
	   for(UploadRequest finalFile:finalFiles) {
		   if(finalFile.getDocTypeCode().equalsIgnoreCase("RECEIPT")) {
			   String tempIdString = finalFile.getInstallmentTempId();
			   tempIdString= tempIdString.replace("inst-", "");
			   Long tempId = Long.parseLong(tempIdString);
			   for(FeeInstallment ins: installments) {
				   
				   if(ins.getInstallmentNo().longValue() == tempId) {
					   finalFile.setInstallmentId(ins.getInstallmentId());
				   }
			   }
			   
		   }
	   }
   }
   
   String url = admissionApiUrlTemplate +"/api/admissions/"+id+"/uploads";
   ResponseEntity<FileUpload[]> response = restTemplate.exchange(
       url, HttpMethod.POST, new HttpEntity<>(finalPayload, headers), FileUpload[].class
   );

   return List.of(response.getBody());
 } catch (Exception e) {
   log.error("Failed to ingest and forward files for admission {}", id, e);
   throw new RuntimeException("Upload failed", e);
 }
}
}

