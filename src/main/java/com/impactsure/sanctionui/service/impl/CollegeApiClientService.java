package com.impactsure.sanctionui.service.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;

import com.impactsure.sanctionui.dto.CollegeDto;
import com.impactsure.sanctionui.dto.CollegeCourseSeatDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CollegeApiClientService {
    private final RestTemplate restTemplate;

    @Value("${admission.service.url}")
    private String admissionBaseUrl;

    private HttpHeaders getHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    public List<CollegeDto> listAll(String accessToken) {
        String url = admissionBaseUrl + "/api/colleges";
        HttpEntity<Void> entity = new HttpEntity<>(getHeaders(accessToken));

        ResponseEntity<CollegeDto[]> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, CollegeDto[].class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return Collections.emptyList();
        }

        return Arrays.asList(response.getBody());
    }

    public Optional<CollegeDto> getById(Long collegeId, String accessToken) {
        String url = admissionBaseUrl + "/api/colleges/" + collegeId;
        HttpEntity<Void> entity = new HttpEntity<>(getHeaders(accessToken));

        try {
            ResponseEntity<CollegeDto> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, CollegeDto.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return Optional.ofNullable(response.getBody());
            }
        } catch (Exception ex) {
            System.err.println("College fetch error: " + ex.getMessage());
        }

        return Optional.empty();
    }

    public CollegeDto save(CollegeDto dto, String accessToken) {
        boolean isUpdate = dto.getCollegeId() != null;
        String url = admissionBaseUrl + "/api/colleges" + (isUpdate ? "/" + dto.getCollegeId() : "");

        HttpHeaders headers = getHeaders(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CollegeDto> entity = new HttpEntity<>(dto, headers);

        ResponseEntity<CollegeDto> response = restTemplate.exchange(
                url,
                isUpdate ? HttpMethod.PUT : HttpMethod.POST,
                entity,
                CollegeDto.class
        );

        return response.getBody();
    }

    public ResponseEntity<String> delete(Long collegeId, String accessToken) {
        String url = admissionBaseUrl + "/api/colleges/" + collegeId;
        HttpEntity<Void> entity = new HttpEntity<>(getHeaders(accessToken));
        try {
            return restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    public List<CollegeCourseSeatDto> getCollegeCourseSeats(Long collegeId, String accessToken) {
        String url = admissionBaseUrl + "/api/colleges/" + collegeId + "/course-seats";
        HttpEntity<Void> entity = new HttpEntity<>(getHeaders(accessToken));

        ResponseEntity<CollegeCourseSeatDto[]> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, CollegeCourseSeatDto[].class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return Collections.emptyList();
        }

        return Arrays.asList(response.getBody());
    }
}
