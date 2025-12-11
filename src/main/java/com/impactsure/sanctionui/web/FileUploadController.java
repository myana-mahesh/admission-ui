package com.impactsure.sanctionui.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import com.impactsure.sanctionui.dto.MultipleUploadRequest;
import com.impactsure.sanctionui.dto.UploadRequest;
import com.impactsure.sanctionui.entities.FileUpload;
import com.impactsure.sanctionui.service.impl.FileIngestService;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admission")
@Slf4j
public class FileUploadController {
	
	 @Value("${admission.service.url}")
	 private String admissionApiUrl;
	 
	 
	 @Autowired
	 private RestTemplate restTemplate;
	 
	 @Autowired
	 private  FileIngestService fileIngestService;
	
	
	  @PostMapping(path = "/{id}/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	  public ResponseEntity<?> addUpload(@PathVariable Long id, 
			    @RequestPart("metadata") MultipleUploadRequest metadata,
		        @RequestPart(name = "files",  required = false) List<MultipartFile> files
		        ,@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
		        @AuthenticationPrincipal OidcUser oidcUser
		        ) {
		  
			log.info("adding files to admission called");
		  	String accessToken = client.getAccessToken().getTokenValue();
//			String accessToken = "";
		  	HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.APPLICATION_JSON);
	        headers.setBearerAuth(accessToken);
	        try {
	        	return ResponseEntity.ok(
	        	        fileIngestService.ingestAndForward(id, metadata, files, accessToken)
	        	    );
			} catch (Exception e) {
				log.error("errpr adding files to admission "+id);
				e.printStackTrace();   
			}
	        return new ResponseEntity<>(null,HttpStatus.INTERNAL_SERVER_ERROR);
	  }

}
