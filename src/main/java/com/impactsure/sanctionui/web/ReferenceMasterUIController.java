package com.impactsure.sanctionui.web;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import com.impactsure.sanctionui.dto.MasterOptionDto;
import com.impactsure.sanctionui.service.impl.ReferenceDataClientService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("")
@RequiredArgsConstructor
public class ReferenceMasterUIController {

    private final ReferenceDataClientService referenceDataClientService;

    @GetMapping("/nationality-master")
    public String showNationalityMaster(Model model,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
            @AuthenticationPrincipal OidcUser oidcUser,
            @ModelAttribute("permissionSet") Set<String> permissionSet) {
        String accessToken = client.getAccessToken().getTokenValue();
        List<MasterOptionDto> options = referenceDataClientService.getNationalities(accessToken);
        model.addAttribute("options", options);
        model.addAttribute("title", "Nationality Master");
        model.addAttribute("subtitle", "Create, edit and delete nationalities used in student profiles.");
        model.addAttribute("entityLabel", "Nationality");
        model.addAttribute("apiBaseUrl", "/nationalities/master");
        model.addAttribute("active", "nationalities");
        model.addAttribute("canManage", canManage(oidcUser, permissionSet, "nationalities:EDIT"));
        return "reference-master";
    }

    @GetMapping("/religion-master")
    public String showReligionMaster(Model model,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
            @AuthenticationPrincipal OidcUser oidcUser,
            @ModelAttribute("permissionSet") Set<String> permissionSet) {
        String accessToken = client.getAccessToken().getTokenValue();
        List<MasterOptionDto> options = referenceDataClientService.getReligions(accessToken);
        model.addAttribute("options", options);
        model.addAttribute("title", "Religion Master");
        model.addAttribute("subtitle", "Create, edit and delete religions used in student profiles.");
        model.addAttribute("entityLabel", "Religion");
        model.addAttribute("apiBaseUrl", "/religions/master");
        model.addAttribute("active", "religions");
        model.addAttribute("canManage", canManage(oidcUser, permissionSet, "religions:EDIT"));
        return "reference-master";
    }

    @GetMapping("/discount-reason-master")
    public String showDiscountReasonMaster(Model model,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
            @AuthenticationPrincipal OidcUser oidcUser,
            @ModelAttribute("permissionSet") Set<String> permissionSet) {
        String accessToken = client.getAccessToken().getTokenValue();
        List<MasterOptionDto> options = referenceDataClientService.getDiscountReasons(accessToken);
        model.addAttribute("options", options);
        model.addAttribute("title", "Discount Reason Master");
        model.addAttribute("subtitle", "Create, edit and delete discount reasons used in admissions.");
        model.addAttribute("entityLabel", "Discount Reason");
        model.addAttribute("apiBaseUrl", "/discount-reasons/master");
        model.addAttribute("active", "discount-reasons");
        model.addAttribute("canManage", canManage(oidcUser, permissionSet, "discount-reasons:EDIT"));
        return "reference-master";
    }

    @GetMapping("/nationalities/master")
    public ResponseEntity<List<MasterOptionDto>> listNationalities(
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client) {
        String accessToken = client.getAccessToken().getTokenValue();
        return ResponseEntity.ok(referenceDataClientService.getNationalities(accessToken));
    }

    @PostMapping("/nationalities/master")
    public ResponseEntity<MasterOptionDto> createNationality(
            @RequestBody MasterOptionDto req,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client) {
        String accessToken = client.getAccessToken().getTokenValue();
        MasterOptionDto created = referenceDataClientService.createNationality(req.getName(), accessToken);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/nationalities/master/{id}")
    public ResponseEntity<MasterOptionDto> updateNationality(
            @PathVariable Long id,
            @RequestBody MasterOptionDto req,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client) {
        String accessToken = client.getAccessToken().getTokenValue();
        MasterOptionDto updated = referenceDataClientService.updateNationality(id, req.getName(), accessToken);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/nationalities/master/{id}")
    public ResponseEntity<String> deleteNationality(
            @PathVariable Long id,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client) {
        String accessToken = client.getAccessToken().getTokenValue();
        ResponseEntity<String> resp = referenceDataClientService.deleteNationality(id, accessToken);
        return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
    }

    @GetMapping("/religions/master")
    public ResponseEntity<List<MasterOptionDto>> listReligions(
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client) {
        String accessToken = client.getAccessToken().getTokenValue();
        return ResponseEntity.ok(referenceDataClientService.getReligions(accessToken));
    }

    @PostMapping("/religions/master")
    public ResponseEntity<MasterOptionDto> createReligion(
            @RequestBody MasterOptionDto req,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client) {
        String accessToken = client.getAccessToken().getTokenValue();
        MasterOptionDto created = referenceDataClientService.createReligion(req.getName(), accessToken);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/religions/master/{id}")
    public ResponseEntity<MasterOptionDto> updateReligion(
            @PathVariable Long id,
            @RequestBody MasterOptionDto req,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client) {
        String accessToken = client.getAccessToken().getTokenValue();
        MasterOptionDto updated = referenceDataClientService.updateReligion(id, req.getName(), accessToken);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/religions/master/{id}")
    public ResponseEntity<String> deleteReligion(
            @PathVariable Long id,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client) {
        String accessToken = client.getAccessToken().getTokenValue();
        ResponseEntity<String> resp = referenceDataClientService.deleteReligion(id, accessToken);
        return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
    }

    @GetMapping("/discount-reasons/master")
    public ResponseEntity<List<MasterOptionDto>> listDiscountReasons(
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client) {
        String accessToken = client.getAccessToken().getTokenValue();
        return ResponseEntity.ok(referenceDataClientService.getDiscountReasons(accessToken));
    }

    @PostMapping("/discount-reasons/master")
    public ResponseEntity<MasterOptionDto> createDiscountReason(
            @RequestBody MasterOptionDto req,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
            @AuthenticationPrincipal OidcUser oidcUser,
            @ModelAttribute("permissionSet") Set<String> permissionSet) {
        ensureCanManage(oidcUser, permissionSet, "discount-reasons:EDIT");
        String accessToken = client.getAccessToken().getTokenValue();
        MasterOptionDto created = referenceDataClientService.createDiscountReason(req.getName(), accessToken);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/discount-reasons/master/{id}")
    public ResponseEntity<MasterOptionDto> updateDiscountReason(
            @PathVariable Long id,
            @RequestBody MasterOptionDto req,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
            @AuthenticationPrincipal OidcUser oidcUser,
            @ModelAttribute("permissionSet") Set<String> permissionSet) {
        ensureCanManage(oidcUser, permissionSet, "discount-reasons:EDIT");
        String accessToken = client.getAccessToken().getTokenValue();
        MasterOptionDto updated = referenceDataClientService.updateDiscountReason(id, req.getName(), accessToken);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/discount-reasons/master/{id}")
    public ResponseEntity<String> deleteDiscountReason(
            @PathVariable Long id,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
            @AuthenticationPrincipal OidcUser oidcUser,
            @ModelAttribute("permissionSet") Set<String> permissionSet) {
        ensureCanManage(oidcUser, permissionSet, "discount-reasons:EDIT");
        String accessToken = client.getAccessToken().getTokenValue();
        ResponseEntity<String> resp = referenceDataClientService.deleteDiscountReason(id, accessToken);
        return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
    }

    private boolean isSuperAdmin(OidcUser oidcUser) {
        if (oidcUser == null) {
            return false;
        }
        return oidcUser.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a));
    }

    private void ensureSuperAdmin(OidcUser oidcUser) {
        if (!isSuperAdmin(oidcUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "SUPER_ADMIN role required");
        }
    }

    private void ensureCanManage(OidcUser oidcUser, Set<String> permissionSet, String requiredPermission) {
        if (canManage(oidcUser, permissionSet, requiredPermission)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "SUPER_ADMIN role required");
    }

    private boolean canManage(OidcUser oidcUser, Set<String> permissionSet, String requiredPermission) {
        if (isSuperAdmin(oidcUser)) {
            return true;
        }
        return permissionSet != null && permissionSet.contains(requiredPermission);
    }
}
