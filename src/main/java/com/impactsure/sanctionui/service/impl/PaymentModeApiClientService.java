package com.impactsure.sanctionui.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.impactsure.sanctionui.dto.PaymentModeDto;
import com.impactsure.sanctionui.entities.PaymentModeMaster;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentModeApiClientService {

    private final RestTemplate restTemplate;

    // Base URL of admission-service, e.g. http://admission-service:8080
    @Value("${admission.service.url}")
    private String admissionBaseUrl;

    // 🟢 Common headers helper
    private HttpHeaders getHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    /** 🔹 Fetch all payment modes (sorted or not) */
    public List<PaymentModeMaster> findAllSorted(String acessToken) {
        String url = admissionBaseUrl + "/api/payment-modes/list";
        HttpEntity<Void> entity = new HttpEntity<>(getHeaders(acessToken));

        ResponseEntity<PaymentModeMaster[]> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, PaymentModeMaster[].class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null)
            return Collections.emptyList();

        return Arrays.asList(response.getBody());
    }

    /** 🔹 Get payment mode by ID */
    public Optional<PaymentModeMaster> findById(Long id,String acessToken) {
        String url = admissionBaseUrl + "/api/payment-modes/byid/" + id;
        HttpEntity<Void> entity = new HttpEntity<>(getHeaders(acessToken));

        try {
            ResponseEntity<PaymentModeMaster> resp =
                    restTemplate.exchange(url, HttpMethod.GET, entity, PaymentModeMaster.class);

            if (resp.getStatusCode().is2xxSuccessful()) {
                return Optional.ofNullable(resp.getBody());
            }
        } catch (Exception ex) {
            System.err.println("⚠️ PaymentMode fetch error: " + ex.getMessage());
        }

        return Optional.empty();
    }

    /** 🔹 Create or update a payment mode */
    public PaymentModeMaster save(PaymentModeMaster pm,String acessToken) {
        String url = admissionBaseUrl + "/api/payment-modes";
        HttpHeaders headers = getHeaders(acessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<PaymentModeMaster> entity = new HttpEntity<>(pm, headers);
        ResponseEntity<PaymentModeMaster> resp =
                restTemplate.exchange(url, HttpMethod.POST, entity, PaymentModeMaster.class);

        return resp.getBody();
    }

    /** 🔹 Delete by ID */
    public void deleteById(Long id,String accessToken) {
        String url = admissionBaseUrl + "/api/payment-modes/deleteById/" + id;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<PaymentModeDto[]> response =
                restTemplate.exchange(url, HttpMethod.DELETE, entity, PaymentModeDto[].class);
    }
    
    public List<PaymentModeDto> getPaymentModes(String accessToken) {

        // 🔹 Adjust path as per your admission-service controller
        String url = admissionBaseUrl + "/api/payment-modes";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<PaymentModeDto[]> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, PaymentModeDto[].class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return Collections.emptyList();
        }

        return Arrays.asList(response.getBody());
    }

    /**
     * Optional: only active modes (if you have a dedicated endpoint)
     */
    public List<PaymentModeMaster> getActivePaymentModes(String accessToken) {
        String url = admissionBaseUrl + "/api/payment-modes/active";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<PaymentModeMaster> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, PaymentModeMaster.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return Collections.emptyList();
        }

        return Arrays.asList(response.getBody());
    }
}
