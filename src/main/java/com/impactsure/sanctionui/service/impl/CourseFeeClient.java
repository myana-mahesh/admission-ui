package com.impactsure.sanctionui.service.impl;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.impactsure.sanctionui.dto.CourseFeeRequestDto;

@Component
@RequiredArgsConstructor
public class CourseFeeClient {

    private final RestTemplate restTemplate;

    @Value("${admission.service.url}")
    private String admissionServiceBaseUrl; // e.g. http://localhost:8081 or http://admission-service

    public CourseFeeRequestDto getCourseWithFee(Long courseId, String accessToken) {
        String url = admissionServiceBaseUrl + "/api/courses/" + courseId + "/with-fee";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken); // Authorization: Bearer <token>

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<CourseFeeRequestDto> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, CourseFeeRequestDto.class);

        return response.getBody();
    }

    public CourseFeeRequestDto createCourseWithFee(CourseFeeRequestDto dto, String accessToken) {
        String url = admissionServiceBaseUrl + "/api/courses/with-fee";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<CourseFeeRequestDto> entity = new HttpEntity<>(dto, headers);

        ResponseEntity<CourseFeeRequestDto> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, CourseFeeRequestDto.class);

        return response.getBody();
    }

    public CourseFeeRequestDto updateCourseWithFee(Long courseId, CourseFeeRequestDto dto, String accessToken) {
        String url = admissionServiceBaseUrl + "/api/courses/" + courseId + "/with-fee";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<CourseFeeRequestDto> entity = new HttpEntity<>(dto, headers);

        ResponseEntity<CourseFeeRequestDto> response =
                restTemplate.exchange(url, HttpMethod.PUT, entity, CourseFeeRequestDto.class);

        return response.getBody();
    }

    public List<CourseFeeRequestDto> getAllCoursesWithFee(String accessToken) {
    	String url = admissionServiceBaseUrl + "/api/courses/with-fee";

    	 HttpHeaders headers = new HttpHeaders();
         headers.setContentType(MediaType.APPLICATION_JSON);
         headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<CourseFeeRequestDto[]> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, CourseFeeRequestDto[].class);

        CourseFeeRequestDto[] body = response.getBody();
        if (body == null || body.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.asList(body);
    }
}
