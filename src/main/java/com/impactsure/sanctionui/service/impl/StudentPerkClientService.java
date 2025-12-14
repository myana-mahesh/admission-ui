package com.impactsure.sanctionui.service.impl;


import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.impactsure.sanctionui.dto.StudentPerkDto;

@Slf4j
@Service
public class StudentPerkClientService {

	@Autowired
	private  RestTemplate restTemplate;

	 @Value("${admission.service.url}") 
	 private String admissionServiceBaseUrl;


 private String perksBaseUrl(Long studentId) {
     return admissionServiceBaseUrl + "/api/students/" + studentId + "/perks";
 }

 /* ===================== GET all perks for a student ===================== */

 public List<StudentPerkDto> getPerksForStudent(Long studentId,String accessToken) {
     String url = perksBaseUrl(studentId);
     
     HttpHeaders headers = new HttpHeaders();
     headers.setContentType(MediaType.APPLICATION_JSON);
     headers.setBearerAuth(accessToken);

     try {
         ResponseEntity<List<StudentPerkDto>> resp =
                 restTemplate.exchange(
                         url,
                         HttpMethod.GET,
                         new HttpEntity(headers),
                         new ParameterizedTypeReference<List<StudentPerkDto>>() {}
                 );

         return resp.getBody() != null ? resp.getBody() : Collections.emptyList();
     } catch (Exception ex) {
         log.error("Error fetching perks for student {}: {}", studentId, ex.getMessage(), ex);
         return Collections.emptyList();
     }
 }

 /* ===================== ASSIGN a single perk ===================== */

 public boolean assignPerkToStudent(Long studentId, Long perkId,String accessToken) {
     String url = perksBaseUrl(studentId) + "/" + perkId;

     HttpHeaders headers = new HttpHeaders();
     headers.setContentType(MediaType.APPLICATION_JSON);
     headers.setBearerAuth(accessToken);

     // If you don't need a body, pass `null` as the body type
     HttpEntity<Void> requestEntity = new HttpEntity<>(null, headers);

     try {
         ResponseEntity<Void> resp =
                 restTemplate.postForEntity(url, requestEntity, Void.class);
         return resp.getStatusCode().is2xxSuccessful();
     } catch (Exception ex) {
         log.error("Error assigning perk {} to student {}: {}", perkId, studentId, ex.getMessage(), ex);
         return false;
     }

 }

 /* ===================== REMOVE a single perk ===================== */

 public boolean removePerkFromStudent(Long studentId, Long perkId,String accessToken) {
     String url = perksBaseUrl(studentId) + "/" + perkId;

     HttpHeaders headers = new HttpHeaders();
     headers.setContentType(MediaType.APPLICATION_JSON);
     headers.setBearerAuth(accessToken);
     
     HttpEntity<Void> requestEntity = new HttpEntity<>(null, headers);

     try {
         ResponseEntity<Void> resp = restTemplate.exchange(
                 url,
                 HttpMethod.DELETE,
                 requestEntity,
                 Void.class
         );
         return resp.getStatusCode().is2xxSuccessful();
     } catch (Exception ex) {
         log.error("Error removing perk {} from student {}: {}", perkId, studentId, ex.getMessage(), ex);
         return false;
     }
 }

 /* ===================== REPLACE all perks for a student ===================== */
 // Calls PUT /api/students/{studentId}/perks with body: [perkId1, perkId2, ...]

 public List<StudentPerkDto> replacePerksForStudent(Long studentId, List<Long> perkIds,String accessToken) {
     String url = perksBaseUrl(studentId);

     HttpHeaders headers = new HttpHeaders();
     headers.setContentType(MediaType.APPLICATION_JSON);
     headers.setBearerAuth(accessToken);

     HttpEntity<List<Long>> entity = new HttpEntity<>(perkIds, headers);

     try {
         ResponseEntity<List<StudentPerkDto>> resp =
                 restTemplate.exchange(
                         url,
                         HttpMethod.PUT,
                         entity,
                         new ParameterizedTypeReference<List<StudentPerkDto>>() {}
                 );

         return resp.getBody() != null ? resp.getBody() : Collections.emptyList();
     } catch (Exception ex) {
         log.error("Error replacing perks for student {}: {}", studentId, ex.getMessage(), ex);
         return Collections.emptyList();
     }
 }
}

