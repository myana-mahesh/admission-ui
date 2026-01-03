package com.impactsure.sanctionui.service.impl;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.impactsure.sanctionui.dto.StudentPerksMasterDto;

@Service
@Slf4j
public class StudentPerksMasterService {

	@Autowired
    private RestTemplate restTemplate;

    
    @Value("${admission.service.url}")
    private String admissionServiceBaseUrl;
    String baseUrl = admissionServiceBaseUrl + "/api/student-perks";
    
    public StudentPerksMasterService() {
    	baseUrl = admissionServiceBaseUrl + "/api/student-perks";
    	
    }
    
   
    private HttpHeaders buildHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (accessToken != null && !accessToken.isBlank()) {
            headers.setBearerAuth(accessToken);
        }
        return headers;
    }

    public List<StudentPerksMasterDto> getAllPerks(String accessToken) {
        String url = admissionServiceBaseUrl + "/api/student-perks";
        try {
        	HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
             headers.setBearerAuth(accessToken);
             
            ResponseEntity<StudentPerksMasterDto[]> resp =
                    restTemplate.exchange(url,HttpMethod.GET,new HttpEntity(headers),StudentPerksMasterDto[].class);

            StudentPerksMasterDto[] body = resp.getBody();
            if (body == null) {
                log.warn("getAllPerks: empty body from {}", url);
                return Collections.emptyList();
            }
            return Arrays.asList(body);
        } catch (Exception ex) {
            log.error("Error calling {}: {}", url, ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }
    
    public StudentPerksMasterDto getPerk(Long perkId, String accessToken) {
    	baseUrl = admissionServiceBaseUrl + "/api/student-perks";
        String url = baseUrl + "/" + perkId;

        try {
            HttpHeaders headers = buildHeaders(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<StudentPerksMasterDto> resp =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            StudentPerksMasterDto.class
                    );

            return resp.getBody();
        } catch (Exception ex) {
            log.error("Error fetching perk {}: {}", perkId, ex.getMessage(), ex);
            return null;
        }
    }

    /* ====================== CREATE ====================== */

    /** Create a new perk in master. */
    public StudentPerksMasterDto createPerk(String name, String accessToken) {
    	baseUrl = admissionServiceBaseUrl + "/api/student-perks";
        String url = baseUrl;

        try {
            HttpHeaders headers = buildHeaders(accessToken);
            Map<String, String> body = Collections.singletonMap("name", name);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<StudentPerksMasterDto> resp =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            StudentPerksMasterDto.class
                    );

            return resp.getBody();
        } catch (Exception ex) {
            log.error("Error creating perk '{}': {}", name, ex.getMessage(), ex);
            return null;
        }
    }

    /* ====================== UPDATE ====================== */

    /** Update an existing perk’s name. */
    public StudentPerksMasterDto updatePerk(Long perkId, String name, String accessToken) {
    	baseUrl = admissionServiceBaseUrl + "/api/student-perks";
        String url = baseUrl + "/" + perkId;

        try {
            HttpHeaders headers = buildHeaders(accessToken);
            Map<String, String> body = Collections.singletonMap("name", name);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<StudentPerksMasterDto> resp =
                    restTemplate.exchange(
                            url,
                            HttpMethod.PUT,
                            entity,
                            StudentPerksMasterDto.class
                    );

            return resp.getBody();
        } catch (Exception ex) {
            log.error("Error updating perk {}: {}", perkId, ex.getMessage(), ex);
            return null;
        }
    }

    /* ====================== DELETE ====================== */

    /** Delete a perk from master. */
    public ResponseEntity<String> deletePerk(Long perkId, String accessToken) {
    	baseUrl = admissionServiceBaseUrl + "/api/student-perks";
        String url = baseUrl + "/" + perkId;

        try {
            HttpHeaders headers = buildHeaders(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> resp =
                    restTemplate.exchange(
                            url,
                            HttpMethod.DELETE,
                            entity,
                            String.class
                    );

            return resp;
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .body(ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("Error deleting perk {}: {}", perkId, ex.getMessage(), ex);
            return ResponseEntity.status(500).body("Unable to delete the perk.");
        }
    }
}
