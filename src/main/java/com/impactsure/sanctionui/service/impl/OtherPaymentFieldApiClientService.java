package com.impactsure.sanctionui.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.impactsure.sanctionui.dto.OtherPaymentFieldDto;
import com.impactsure.sanctionui.dto.OtherPaymentFieldValueRequest;
import com.impactsure.sanctionui.dto.StudentOtherPaymentValueDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OtherPaymentFieldApiClientService {

    private final RestTemplate restTemplate;

    @Value("${admission.service.url}")
    private String baseUrl;

    public List<OtherPaymentFieldDto> listFields(boolean includeInactive, String accessToken) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/api/other-payments/fields")
                .queryParam("includeInactive", includeInactive)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<List<OtherPaymentFieldDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody() == null ? List.of() : response.getBody();
    }

    public OtherPaymentFieldDto createField(OtherPaymentFieldDto payload, String accessToken) {
        return saveField(null, payload, accessToken);
    }

    public OtherPaymentFieldDto updateField(Long id, OtherPaymentFieldDto payload, String accessToken) {
        return saveField(id, payload, accessToken);
    }

    public void deleteField(Long id, String accessToken) {
        String url = baseUrl + "/api/other-payments/fields/" + id;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
    }

    public List<StudentOtherPaymentValueDto> listStudentValues(Long studentId, String accessToken) {
        String url = baseUrl + "/api/other-payments/students/" + studentId;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<List<StudentOtherPaymentValueDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody() == null ? List.of() : response.getBody();
    }

    public void saveStudentValues(Long studentId, List<OtherPaymentFieldValueRequest> values, String accessToken) {
        String url = baseUrl + "/api/other-payments/students/" + studentId;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<OtherPaymentFieldValueRequest>> entity = new HttpEntity<>(values, headers);
        restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
    }

    private OtherPaymentFieldDto saveField(Long id, OtherPaymentFieldDto payload, String accessToken) {
        String url = baseUrl + "/api/other-payments/fields" + (id == null ? "" : "/" + id);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<OtherPaymentFieldDto> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<OtherPaymentFieldDto> response = restTemplate.exchange(
                url,
                id == null ? HttpMethod.POST : HttpMethod.PUT,
                entity,
                OtherPaymentFieldDto.class
        );
        return response.getBody();
    }
}
