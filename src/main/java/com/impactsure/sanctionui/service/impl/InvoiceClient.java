
package com.impactsure.sanctionui.service.impl;

import com.impactsure.sanctionui.dto.FeeInvoiceDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Service
public class InvoiceClient {

    private final RestTemplate restTemplate;

    @Value("${admission.service.url}")
    private String admissionServiceBaseUrl;   // e.g. http://admission-service:8081

    private String invoicesByAdmissionUrl(Long admissionId) {
        return admissionServiceBaseUrl + "/api/invoices/by-admission/" + admissionId;
    }

    public List<FeeInvoiceDto> getInvoicesForAdmission(Long admissionId, String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<FeeInvoiceDto>> resp =
                    restTemplate.exchange(
                            invoicesByAdmissionUrl(admissionId),
                            HttpMethod.GET,
                            entity,
                            new ParameterizedTypeReference<List<FeeInvoiceDto>>() {}
                    );

            return resp.getBody() != null ? resp.getBody() : Collections.emptyList();
        } catch (Exception ex) {
            log.error("Error fetching invoices for admission {}: {}", admissionId, ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }
}
