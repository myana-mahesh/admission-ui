package com.impactsure.sanctionui.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.impactsure.sanctionui.dto.OtherPaymentFieldDto;
import com.impactsure.sanctionui.service.impl.OtherPaymentFieldApiClientService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/other-payment-fields")
@RequiredArgsConstructor
public class OtherPaymentFieldUIController {

    private final OtherPaymentFieldApiClientService otherPaymentFieldApiClientService;

    @GetMapping
    public String listPage(Model model,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
            @AuthenticationPrincipal OidcUser oidcUser) {
        String accessToken = client.getAccessToken().getTokenValue();
        List<OtherPaymentFieldDto> fields = otherPaymentFieldApiClientService.listFields(true, accessToken);
        model.addAttribute("fields", fields);
        model.addAttribute("active", "other-payment-fields");
        model.addAttribute("role", getSingleRole(clientRoleNames(oidcUser)));
        model.addAttribute("userName", oidcUser != null ? oidcUser.getFullName() : null);
        return "other-payment-fields";
    }

    @GetMapping("/master")
    public ResponseEntity<List<OtherPaymentFieldDto>> list(
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client) {
        String accessToken = client.getAccessToken().getTokenValue();
        return ResponseEntity.ok(otherPaymentFieldApiClientService.listFields(true, accessToken));
    }

    @PostMapping("/master")
    public ResponseEntity<OtherPaymentFieldDto> create(
            @RequestBody OtherPaymentFieldDto req,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client) {
        String accessToken = client.getAccessToken().getTokenValue();
        return ResponseEntity.ok(otherPaymentFieldApiClientService.createField(req, accessToken));
    }

    @PutMapping("/master/{id}")
    public ResponseEntity<OtherPaymentFieldDto> update(
            @PathVariable Long id,
            @RequestBody OtherPaymentFieldDto req,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client) {
        String accessToken = client.getAccessToken().getTokenValue();
        return ResponseEntity.ok(otherPaymentFieldApiClientService.updateField(id, req, accessToken));
    }

    @DeleteMapping("/master/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client) {
        String accessToken = client.getAccessToken().getTokenValue();
        otherPaymentFieldApiClientService.deleteField(id, accessToken);
        return ResponseEntity.noContent().build();
    }

    private List<String> clientRoleNames(OidcUser user) {
        if (user == null) {
            return List.of();
        }
        return user.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .toList();
    }

    private String getSingleRole(List<String> roles) {
        String role = "";
        if (roles.contains("SUPER_ADMIN")) {
            role = "SUPER_ADMIN";
        } else if (roles.contains("ADMIN")) {
            role = "ADMIN";
        } else if (roles.contains("HO")) {
            role = "HO";
        } else if (roles.contains("BRANCH_USER")) {
            role = "BRANCH_USER";
        }
        return role;
    }
}
