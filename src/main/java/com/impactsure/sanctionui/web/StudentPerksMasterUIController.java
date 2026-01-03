package com.impactsure.sanctionui.web;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.impactsure.sanctionui.dto.StudentPerkDto;
import com.impactsure.sanctionui.dto.StudentPerksMasterDto;
import com.impactsure.sanctionui.service.impl.StudentPerksMasterService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class StudentPerksMasterUIController {

	@Autowired
    private StudentPerksMasterService perksClient;

    @GetMapping("/perks-master")
    public String showPerksMaster(Model model,@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
	        @AuthenticationPrincipal OidcUser oidcUser
	        ) {
		
		 String accessToken = client.getAccessToken().getTokenValue();
        List<StudentPerksMasterDto> perks = perksClient.getAllPerks(accessToken);
        model.addAttribute("perks", perks);
        model.addAttribute("perksApiBaseUrl", "/perks/master");
        return "perskmaster";
    }

 // GET /perks/master  -> used by loadPerks()
    @GetMapping("/perks/master")
    public ResponseEntity<List<StudentPerksMasterDto>> listPerks(@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
	        @AuthenticationPrincipal OidcUser oidcUser
	        ) {
    	String accessToken = client.getAccessToken().getTokenValue();
        List<StudentPerksMasterDto> perks = perksClient.getAllPerks(accessToken);
        return ResponseEntity.ok(perks);
    }

    // POST /perks/master  -> used by createPerk()
    @PostMapping("/perks/master")
    public ResponseEntity<StudentPerksMasterDto> createPerk(@RequestBody StudentPerkDto req,@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
	        @AuthenticationPrincipal OidcUser oidcUser
	        ) {
    	 String accessToken = client.getAccessToken().getTokenValue();
        StudentPerksMasterDto created = perksClient.createPerk(req.getPerkName(), accessToken);
        return ResponseEntity.ok(created);
    }

    // PUT /perks/master/{id}  -> used by updatePerk()
    @PutMapping("/perks/master/{id}")
    public ResponseEntity<StudentPerksMasterDto> updatePerk(
            @PathVariable Long id,
            @RequestBody StudentPerkDto req
            ,@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
	        @AuthenticationPrincipal OidcUser oidcUser
	        ) {
    	 String accessToken = client.getAccessToken().getTokenValue();
    	 StudentPerksMasterDto updated = perksClient.updatePerk(id, req.getPerkName(), accessToken);
        return ResponseEntity.ok(updated);
    }

    // DELETE /perks/master/{id}  -> used by deletePerk()
    @DeleteMapping("/perks/master/{id}")
    public ResponseEntity<String> deletePerk(@PathVariable Long id,@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
	        @AuthenticationPrincipal OidcUser oidcUser
	        ) {
		
		 String accessToken = client.getAccessToken().getTokenValue();
        ResponseEntity<String> resp = perksClient.deletePerk(id, accessToken);
        return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
    }
}
