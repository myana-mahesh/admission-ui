package com.impactsure.sanctionui.service.impl;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.impactsure.sanctionui.dto.FeeInstallmentInvoiceResponse;
import com.impactsure.sanctionui.dto.FeeInstallmentStatusUpdateRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeeInstallmentUiClient {

    private final RestTemplate restTemplate;

    /**
     * Base URL of admission-service (the backend where FeeInstallment + invoices live)
     * Example: http://admission-service:8080  or  http://localhost:8081
     */
    @Value("${admission.service.url}")
    private String admissionServiceBaseUrl;

    private String statusUrl(Long installmentId) {
        // admission-service endpoint: POST /api/fee-installments/{installmentId}/status
        return admissionServiceBaseUrl + "/api/fee-installments/" + installmentId + "/status";
    }

    /**
     * Generic method to change status of an installment.
     * If status == "Paid", this will also generate (or reuse) invoice and
     * return its download URL.
     */
    public FeeInstallmentInvoiceResponse updateInstallmentStatus(
            Long installmentId,
            String status,
            String accessToken
    ) {
        String url = statusUrl(installmentId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (accessToken != null && !accessToken.isBlank()) {
            headers.setBearerAuth(accessToken);
        }

        FeeInstallmentStatusUpdateRequest payload =
                new FeeInstallmentStatusUpdateRequest(status);

        HttpEntity<FeeInstallmentStatusUpdateRequest> entity =
                new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<FeeInstallmentInvoiceResponse> resp =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            FeeInstallmentInvoiceResponse.class
                    );

            if (resp.getStatusCode().is2xxSuccessful()) {
                return resp.getBody();
            } else {
                log.error("Failed to update installment {} status. HTTP {}",
                        installmentId, resp.getStatusCodeValue());
                return null;
            }
        } catch (Exception ex) {
            log.error("Error calling admission-service for installment {}: {}",
                    installmentId, ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Convenience wrapper: mark an installment as "Paid".
     * Returns invoice response (with downloadUrl) or null if failed.
     */
    public FeeInstallmentInvoiceResponse markInstallmentPaid(
            Long installmentId,
            String accessToken
    ) {
        return updateInstallmentStatus(installmentId, "Paid", accessToken);
    }

    /**
     * Convenience wrapper: mark installment as "Un Paid" (no invoice generation).
     */
    public FeeInstallmentInvoiceResponse markInstallmentUnpaid(
            Long installmentId,
            String accessToken
    ) {
        return updateInstallmentStatus(installmentId, "Un Paid", accessToken);
    }
    
    public Map deleteInstallment(Long installmentId, boolean deleteFilesAlso, String bearerTokenIfAny) {
        String url = UriComponentsBuilder
                .fromHttpUrl(admissionServiceBaseUrl)
                .path("/api/fee-installments/{id}")
                .queryParam("deleteFilesAlso", deleteFilesAlso)
                .buildAndExpand(installmentId)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        // If your Admission Service is secured (JWT)
        if (bearerTokenIfAny != null && !bearerTokenIfAny.isBlank()) {
            headers.setBearerAuth(bearerTokenIfAny);
        }

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> res = restTemplate.exchange(url, HttpMethod.DELETE, entity, Map.class);
            return res.getBody();
        } catch (HttpStatusCodeException ex) {
            // Return backend error JSON (if any) to UI caller
            String body = ex.getResponseBodyAsString();
            throw new RuntimeException("Admission API failed (" + ex.getStatusCode() + "): " + body, ex);
        }
    }
}
