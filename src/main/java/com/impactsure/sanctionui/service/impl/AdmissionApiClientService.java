package com.impactsure.sanctionui.service.impl;

import com.impactsure.sanctionui.dto.CancelAdmissionDTO;
import com.impactsure.sanctionui.dto.CreateAdmissionRequest;
import com.impactsure.sanctionui.dto.OfficeUpdateRequest;
import com.impactsure.sanctionui.entities.Admission2;
import com.impactsure.sanctionui.entities.FileUpload;
import com.impactsure.sanctionui.enums.AdmissionStatus;
import com.impactsure.sanctionui.repository.Admission2Repository;
import com.impactsure.sanctionui.repository.FileUploadRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.impactsure.sanctionui.repository.AdmissionSpecifications.keywordLike;
import static com.impactsure.sanctionui.repository.AdmissionSpecifications.statusIn;

@Service
public class AdmissionApiClientService {

    private final RestTemplate restTemplate;
    
    @Autowired
    private  Admission2Repository admission2Repository;
    
    @Autowired
    private  FileUploadRepository fileRepo;

    @Value("${admission.service.url}")
    private String admissionApiUrl; // e.g., http://localhost:8087/api/admission

    public AdmissionApiClientService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Admission2 createAdmission( CreateAdmissionRequest req, String accessToken ) {

        // 3️⃣ Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<CreateAdmissionRequest> requestEntity = new HttpEntity<>(req, headers);

        // 4️⃣ Call the API
        try {
        	 ResponseEntity<Admission2> response = restTemplate.postForEntity(
                     admissionApiUrl+"/api/admissions",
                     requestEntity,
                     Admission2.class
             );
        	 return response.getBody();
		} catch (Exception e) {
			e.printStackTrace();
		}
    

        // 5️⃣ Return response body
        return null;
    }
    
    public Admission2 acknowledgeAdmission( Long id, String accessToken ) {

        // 3️⃣ Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<CreateAdmissionRequest> requestEntity = new HttpEntity<>(headers);

        // 4️⃣ Call the API
        try {
        	 ResponseEntity<Admission2> response = restTemplate.postForEntity(
                     admissionApiUrl+"/api/admissions/send-acknowledgement?id="+id,
                     requestEntity,
                     Admission2.class
             );
        	 return response.getBody();
		} catch (Exception e) {
			e.printStackTrace();
		}
  
        return null;
    }
    
   
    public Page<Admission2> searchAdmissions(String q, String statusCsv, int page, int size) {
        int pg = Math.max(0, page);
        int sz = Math.min(Math.max(1, size), 100);

        List<AdmissionStatus> statuses = parseStatusesOrDefault(statusCsv);
        
        Specification<Admission2> spec = Specification
                .where(statusIn(statuses))
                .and(keywordLike(q));

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt", "admissionId");
        Pageable pageable = PageRequest.of(pg, sz, sort);

        return admission2Repository.findAll(spec, pageable);
    }

    private List<AdmissionStatus> parseStatusesOrDefault(String statusCsv) {
        if (statusCsv == null || statusCsv.isBlank()) {
            return List.of(AdmissionStatus.PENDING, AdmissionStatus.SUCCESS);
        }
        String[] parts = statusCsv.split(",");
        List<AdmissionStatus> parsed = Arrays.stream(parts)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(this::safeEnumIgnoreCase)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        return parsed.isEmpty() ? List.of(AdmissionStatus.PENDING, AdmissionStatus.SUCCESS) : parsed;
    }

    
    private AdmissionStatus safeEnumIgnoreCase(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        for (AdmissionStatus as : AdmissionStatus.values()) {
            if (as.name().equalsIgnoreCase(s)) {
                return as;
            }
        }
        // Optional: handle common aliases if you ever add them
        // if ("SUCCESSFUL".equalsIgnoreCase(s)) return AdmissionStatus.Success;
        return null;
    }

    public Admission2 getAdmissionById(Long id) {
        return admission2Repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admission not found: " + id));
    }
    
    
    public Admission2 updateAdmission(Long id, Admission2 updated) {
        Admission2 existing = getAdmissionById(id);

        existing.setFormNo(updated.getFormNo());
        existing.setFormDate(updated.getFormDate());
        existing.setDateOfAdm(updated.getDateOfAdm());
        existing.setLastCollege(updated.getLastCollege());
        existing.setCollegeAttended(updated.getCollegeAttended());
        existing.setCollegeLocation(updated.getCollegeLocation());
        existing.setRemarks(updated.getRemarks());
        existing.setTotalFees(updated.getTotalFees());
        existing.setDiscount(updated.getDiscount());
        existing.setNoOfInstallments(updated.getNoOfInstallments());
        existing.setStatus(updated.getStatus());

        return admission2Repository.save(existing);
    }

    public Map<Long, FileUpload> findReceiptMapForAdmission(Long admissionId) {
        List<FileUpload> files = fileRepo.findByAdmissionAdmissionIdAndInstallmentNotNull(admissionId);
        return files.stream()
                .collect(Collectors.toMap(f -> f.getInstallment().getInstallmentId(), Function.identity(),
                                          (a,b) -> a)); // keep first if multiple
    }
    
    public Map<String, FileUpload> findDocUploadMapForAdmission(Long admissionId) {
        List<FileUpload> files = fileRepo.findByAdmissionAdmissionIdAndDocTypeCodeNot(admissionId,"RECEIPT");
        return files.stream()
                .collect(Collectors.toMap(f -> f.getDocType().getCode(), Function.identity(),
                                          (a,b) -> a)); // keep first if multiple
    }


    public String cancelAdmission(CancelAdmissionDTO dto, String accessToken) {

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        // Attach DTO + headers
        HttpEntity<CancelAdmissionDTO> requestEntity = new HttpEntity<>(dto, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    admissionApiUrl + "/admissions/cancel-admission",
                    requestEntity,
                    String.class
            );

            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public CancelAdmissionDTO fetchCancelAdmissionDetails(Long admissionId, String accessToken) {

        try {
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<CancelAdmissionDTO> response =
                    restTemplate.exchange(
                            admissionApiUrl
                                    + "/admissions/fetch-cancel-admission-details/"
                                    + admissionId,
                            HttpMethod.GET,
                            requestEntity,
                            CancelAdmissionDTO.class
                    );


            return response.getBody();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public CancelAdmissionDTO cancelAdmissionDetailsUpdate(
            Long admissionId,
            Double cancelCharges,
            String handlingPerson,
            String remark,
            String refundProofFileName,
            String accessToken) {

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            CancelAdmissionDTO dto = new CancelAdmissionDTO();
            dto.setAdmissionId(admissionId);
            dto.setCancelCharges(cancelCharges);
            dto.setHandlingPerson(handlingPerson);
            dto.setRemark(remark);
            dto.setRefundProofFileName(refundProofFileName);

            HttpEntity<CancelAdmissionDTO> requestEntity =
                    new HttpEntity<>(dto, headers);

            ResponseEntity<CancelAdmissionDTO> response =
                    restTemplate.exchange(
                            admissionApiUrl + "/admissions/cancel-admission-details-update",
                            HttpMethod.PUT,
                            requestEntity,
                            CancelAdmissionDTO.class
                    );

            return response.getBody();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}
