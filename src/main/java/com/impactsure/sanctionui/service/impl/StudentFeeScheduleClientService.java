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

import com.impactsure.sanctionui.dto.CreateStudentFeeScheduleRequest;
import com.impactsure.sanctionui.dto.StudentFeeScheduleDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentFeeScheduleClientService {

    private final RestTemplate restTemplate;

    @Value("${admission.service.url}")
    private String baseUrl;

    public List<StudentFeeScheduleDto> getSchedulesByStudentId(Long studentId, String accessToken) {
        String url = baseUrl + "/api/fees/schedules/student/" + studentId;

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<List<StudentFeeScheduleDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody();
    }

    public List<StudentFeeScheduleDto> getPendingSchedulesByStudentId(Long studentId, String accessToken) {
        String url = baseUrl + "/api/fees/schedules/student/" + studentId + "/pending";

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<List<StudentFeeScheduleDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody();
    }

    public StudentFeeScheduleDto createSchedule(CreateStudentFeeScheduleRequest request, String accessToken) {
        String url = baseUrl + "/api/fees/schedules";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(accessToken);

        HttpEntity<CreateStudentFeeScheduleRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<StudentFeeScheduleDto> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                StudentFeeScheduleDto.class
        );
        return response.getBody();
    }

    public StudentFeeScheduleDto updateScheduleStatus(Long scheduleId, String status, String completedBy, String accessToken) {
        String url = baseUrl + "/api/fees/schedules/" + scheduleId + "/status?status=" + status;
        if (completedBy != null && !completedBy.isEmpty()) {
            url += "&completedBy=" + completedBy;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<StudentFeeScheduleDto> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                entity,
                StudentFeeScheduleDto.class
        );
        return response.getBody();
    }

    public void deleteSchedule(Long scheduleId, String accessToken) {
        String url = baseUrl + "/api/fees/schedules/" + scheduleId;

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

    public Long getPendingCount(Long studentId, String accessToken) {
        String url = baseUrl + "/api/fees/schedules/student/" + studentId + "/count";

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
