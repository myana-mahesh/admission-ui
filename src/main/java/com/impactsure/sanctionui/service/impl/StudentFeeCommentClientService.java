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

import com.impactsure.sanctionui.dto.CreateStudentFeeCommentRequest;
import com.impactsure.sanctionui.dto.StudentFeeCommentDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentFeeCommentClientService {

    private final RestTemplate restTemplate;

    @Value("${admission.service.url}")
    private String baseUrl;

    public List<StudentFeeCommentDto> getCommentsByStudentId(Long studentId, String accessToken) {
        String url = baseUrl + "/api/fees/comments/student/" + studentId;

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<List<StudentFeeCommentDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody();
    }

    public StudentFeeCommentDto createComment(CreateStudentFeeCommentRequest request, String accessToken) {
        String url = baseUrl + "/api/fees/comments";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(accessToken);

        HttpEntity<CreateStudentFeeCommentRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<StudentFeeCommentDto> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                StudentFeeCommentDto.class
        );
        return response.getBody();
    }

    public void deleteComment(Long commentId, String accessToken) {
        String url = baseUrl + "/api/fees/comments/" + commentId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                entity,
                Void.class
        );
    }

    public Long getCommentCount(Long studentId, String accessToken) {
        String url = baseUrl + "/api/fees/comments/student/" + studentId + "/count";

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Long> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Long.class
        );
        return response.getBody();
    }
}
