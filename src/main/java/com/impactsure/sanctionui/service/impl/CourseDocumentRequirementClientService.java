package com.impactsure.sanctionui.service.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.impactsure.sanctionui.dto.CourseDocumentRequirementDto;
import com.impactsure.sanctionui.dto.CourseDocumentRequirementRequest;
import com.impactsure.sanctionui.dto.DocumentTypeOptionDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseDocumentRequirementClientService {

    private final RestTemplate restTemplate;

    @Value("${admission.service.url}")
    private String admissionServiceBaseUrl;

    private HttpHeaders buildHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (accessToken != null && !accessToken.isBlank()) {
            headers.setBearerAuth(accessToken);
        }
        return headers;
    }

    public List<CourseDocumentRequirementDto> listRequirements(String accessToken) {
        String url = admissionServiceBaseUrl + "/api/course-documents";
        try {
            HttpHeaders headers = buildHeaders(accessToken);
            ResponseEntity<CourseDocumentRequirementDto[]> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), CourseDocumentRequirementDto[].class);
            CourseDocumentRequirementDto[] body = resp.getBody();
            if (body == null) {
                return Collections.emptyList();
            }
            return Arrays.asList(body);
        } catch (Exception ex) {
            log.error("Error fetching course document requirements: {}", ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }

    public CourseDocumentRequirementDto createRequirement(CourseDocumentRequirementRequest request,
            String accessToken) {
        String url = admissionServiceBaseUrl + "/api/course-documents";
        try {
            HttpHeaders headers = buildHeaders(accessToken);
            HttpEntity<CourseDocumentRequirementRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<CourseDocumentRequirementDto> resp = restTemplate.exchange(
                    url, HttpMethod.POST, entity, CourseDocumentRequirementDto.class);
            return resp.getBody();
        } catch (Exception ex) {
            log.error("Error creating course document requirement: {}", ex.getMessage(), ex);
            return null;
        }
    }

    public CourseDocumentRequirementDto updateRequirement(Long id, CourseDocumentRequirementRequest request,
            String accessToken) {
        String url = admissionServiceBaseUrl + "/api/course-documents/" + id;
        try {
            HttpHeaders headers = buildHeaders(accessToken);
            HttpEntity<CourseDocumentRequirementRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<CourseDocumentRequirementDto> resp = restTemplate.exchange(
                    url, HttpMethod.PUT, entity, CourseDocumentRequirementDto.class);
            return resp.getBody();
        } catch (Exception ex) {
            log.error("Error updating course document requirement {}: {}", id, ex.getMessage(), ex);
            return null;
        }
    }

    public boolean deleteRequirement(Long id, String accessToken) {
        String url = admissionServiceBaseUrl + "/api/course-documents/" + id;
        try {
            HttpHeaders headers = buildHeaders(accessToken);
            ResponseEntity<Void> resp = restTemplate.exchange(
                    url, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
            return resp.getStatusCode().is2xxSuccessful();
        } catch (Exception ex) {
            log.error("Error deleting course document requirement {}: {}", id, ex.getMessage(), ex);
            return false;
        }
    }

    public List<DocumentTypeOptionDto> listDocumentTypes(String accessToken) {
        String url = admissionServiceBaseUrl + "/api/lookup/doc-types";
        try {
            HttpHeaders headers = buildHeaders(accessToken);
            ResponseEntity<DocumentTypeOptionDto[]> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), DocumentTypeOptionDto[].class);
            DocumentTypeOptionDto[] body = resp.getBody();
            if (body == null) {
                return Collections.emptyList();
            }
            return Arrays.asList(body);
        } catch (Exception ex) {
            log.error("Error fetching document types: {}", ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }
}
