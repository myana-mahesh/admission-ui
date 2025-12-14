package com.impactsure.sanctionui.web;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.impactsure.sanctionui.dto.StudentPerkDto;
import com.impactsure.sanctionui.service.impl.StudentPerkClientService;

@RestController
@RequestMapping("/student")
public class StudentPerksController {
	
	@Autowired
	private StudentPerkClientService studentPerkClientService;
	
	@PutMapping("/{studentId}/perks")
    public ResponseEntity<List<StudentPerkDto>> replacePerks(
            @PathVariable Long studentId,
            @RequestBody List<Long> perkIds
            ,@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
	        @AuthenticationPrincipal OidcUser oidcUser
	        ) {
		
		 String accessToken = client.getAccessToken().getTokenValue();
		List<StudentPerkDto> assignedPerks = this.studentPerkClientService.replacePerksForStudent(studentId, perkIds, accessToken);
		return new ResponseEntity<List<StudentPerkDto>>(assignedPerks,HttpStatus.OK);
	}

}
