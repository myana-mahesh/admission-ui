package com.impactsure.sanctionui.service.impl;

import lombok.RequiredArgsConstructor;

import org.keycloak.representations.AccessToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.impactsure.sanctionui.dto.CreateStudentRequest;
import com.impactsure.sanctionui.dto.PagedResponse;
import com.impactsure.sanctionui.dto.StudentDto;
import com.impactsure.sanctionui.entities.Student;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentApiClientService {

    private final RestTemplate restTemplate;

    // Example: http://localhost:8080/api/students
    @Value("${admission.service.url}")
    private String studentApiUrl;

    public Student createOrUpdateStudent(CreateStudentRequest request, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // If your endpoint needs auth, add it here:
         headers.setBearerAuth(accessToken);

        HttpEntity<CreateStudentRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<Student> response = restTemplate.exchange(
                    studentApiUrl+"/api/students",                 // POST to /api/students (your @PostMapping)
                    HttpMethod.POST,
                    entity,
                    Student.class
            );
            if(response!=null && response.getStatusCode().is2xxSuccessful()) {
            	
            }
            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            // Log server response for debugging
            String body = ex.getResponseBodyAsString();
            throw new RuntimeException(
                    "Student API call failed: HTTP " + ex.getStatusCode() + " - " + body, ex);
        } catch (Exception ex) {
            throw new RuntimeException("Student API call failed: " + ex.getMessage(), ex);
        }
    }
    
    public PagedResponse<StudentDto> getStudents(int page, int size, String q, String accessToken) {

        // Build URL: /api/students?page=&size=&q=
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(studentApiUrl + "/api/students")
                .queryParam("page", page)
                .queryParam("size", size);

        if (q != null && !q.isBlank()) {
            builder.queryParam("q", q);
        }

        String url = builder.toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(accessToken);

        // If protected by Keycloak / JWT, set Authorization here:
        // headers.setBearerAuth(tokenIfAny);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<PagedResponse<StudentDto>> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<PagedResponse<StudentDto>>() {}
                );

        return response.getBody();
    }
    
    public StudentDto getStudentById(Long id, String accessToken) {
        String url = studentApiUrl + "/api/students/{id}"; // admission-service endpoint

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<StudentDto> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<StudentDto>() {},
                id
        );
        return response.getBody();
    }

    public PagedResponse<StudentDto> getStudentsByFilter(
            int page,
            int size,
            String q,
            String courseId,
            String collegeId,
            String batch,
            String admissionYearId,
            String perkId,
            String gender,
            String accessToken
    ) {

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(studentApiUrl + "/api/students/students-filter")
                .queryParam("page", page)
                .queryParam("size", size);

        if (q != null && !q.isBlank())
            builder.queryParam("q", q);

        if (courseId != null)
            builder.queryParam("courseId", courseId);

        if (collegeId != null)
            builder.queryParam("collegeId", collegeId);

        if (batch != null)
            builder.queryParam("batch", batch);

        if (admissionYearId != null)
            builder.queryParam("admissionYearId", admissionYearId);

        if (perkId != null)
            builder.queryParam("perkId", perkId);

        if (gender != null)
            builder.queryParam("gender", gender);

        String url = builder.toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<PagedResponse<StudentDto>> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<>() {}
                );

        return response.getBody();
    }

}
