package com.impactsure.sanctionui.web;

import org.keycloak.representations.AccessToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/auth")
public class KeycloakController {

	@Value("${keycloak.auth-server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String keycloakRealm;

    @Value("${app.url}")
    private String keycloakRedirectUri;

    @GetMapping("/logout1")
    public RedirectView logout(@RequestParam(value = "redirect_uri", required = false, defaultValue = "/") String redirectUri) {
        // URL encode the redirect_uri to handle any special characters
        String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

        // Build the Keycloak logout URL
        String keycloakLogoutUrl = String.format("%s/realms/%s/protocol/openid-connect/logout?redirect_uri=%s",
                keycloakUrl, keycloakRealm, encodedRedirectUri);

        // Redirect to the Keycloak logout URL
        RedirectView redirectView = new RedirectView();
        redirectView.setUrl(keycloakLogoutUrl);

        return redirectView;
    }
    @GetMapping("/logout")
    public RedirectView logout(Principal principal,HttpServletRequest request,
    		@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
	        @AuthenticationPrincipal OidcUser oidcUser) {
       
        try {
        	String encodedRedirectUri = URLEncoder.encode(keycloakRedirectUri, StandardCharsets.UTF_8);

        	OidcIdToken idToken = oidcUser.getIdToken(); // This should be fetched from your OAuth2 token (e.g., from the session)
        	String a= client.getPrincipalName();
            // Build the Keycloak logout URL
            String keycloakLogoutUrl = String.format("%s/realms/%s/protocol/openid-connect/logout?post_logout_redirect_uri=%s&id_token_hint=%s",
                    keycloakUrl, keycloakRealm, encodedRedirectUri, "f4a68975-c61f-4a11-98e0-34e2ee040159");


			request.logout();
		} catch (ServletException e) {
			e.printStackTrace();
		}
        RedirectView redirectView = new RedirectView();
        redirectView.setUrl(keycloakRedirectUri);  // redirect after Keycloak logout

        return redirectView;
    }
}