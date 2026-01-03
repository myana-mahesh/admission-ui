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

import com.impactsure.sanctionui.dto.MasterOptionDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReferenceDataClientService {

    private final RestTemplate restTemplate;

    @Value("${admission.service.url}")
    private String baseUrl;

    public List<MasterOptionDto> getNationalities(String accessToken) {
        return fetchList("/api/reference/nationalities", accessToken);
    }

    public List<MasterOptionDto> getReligions(String accessToken) {
        return fetchList("/api/reference/religions", accessToken);
    }

    public List<MasterOptionDto> getDiscountReasons(String accessToken) {
        return fetchList("/api/discount-reasons", accessToken);
    }

    public MasterOptionDto createNationality(String name, String accessToken) {
        return postItem("/api/reference/nationalities", name, accessToken);
    }

    public MasterOptionDto updateNationality(Long id, String name, String accessToken) {
        return putItem("/api/reference/nationalities/" + id, name, accessToken);
    }

    public ResponseEntity<String> deleteNationality(Long id, String accessToken) {
        return deleteItem("/api/reference/nationalities/" + id, accessToken);
    }

    public MasterOptionDto createDiscountReason(String name, String accessToken) {
        return postItem("/api/discount-reasons", name, accessToken);
    }

    public MasterOptionDto updateDiscountReason(Long id, String name, String accessToken) {
        return putItem("/api/discount-reasons/" + id, name, accessToken);
    }

    public ResponseEntity<String> deleteDiscountReason(Long id, String accessToken) {
        return deleteItem("/api/discount-reasons/" + id, accessToken);
    }

    public MasterOptionDto createReligion(String name, String accessToken) {
        return postItem("/api/reference/religions", name, accessToken);
    }

    public MasterOptionDto updateReligion(Long id, String name, String accessToken) {
        return putItem("/api/reference/religions/" + id, name, accessToken);
    }

    public ResponseEntity<String> deleteReligion(Long id, String accessToken) {
        return deleteItem("/api/reference/religions/" + id, accessToken);
    }

    private List<MasterOptionDto> fetchList(String path, String accessToken) {
        String url = baseUrl + path;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<List<MasterOptionDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody() == null ? List.of() : response.getBody();
    }

    private MasterOptionDto postItem(String path, String name, String accessToken) {
        String url = baseUrl + path;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<?> entity = new HttpEntity<>(java.util.Map.of("name", name), headers);
        ResponseEntity<MasterOptionDto> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                MasterOptionDto.class
        );
        return response.getBody();
    }

    private MasterOptionDto putItem(String path, String name, String accessToken) {
        String url = baseUrl + path;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<?> entity = new HttpEntity<>(java.util.Map.of("name", name), headers);
        ResponseEntity<MasterOptionDto> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                entity,
                MasterOptionDto.class
        );
        return response.getBody();
    }

    private ResponseEntity<String> deleteItem(String path, String accessToken) {
        String url = baseUrl + path;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    String.class
            );
            return response;
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .body(ex.getResponseBodyAsString());
        }
    }
}
