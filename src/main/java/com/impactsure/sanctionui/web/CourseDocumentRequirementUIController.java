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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;

import com.impactsure.sanctionui.dto.CourseDocumentRequirementDto;
import com.impactsure.sanctionui.dto.CourseDocumentRequirementRequest;
import com.impactsure.sanctionui.dto.CourseFeeRequestDto;
import com.impactsure.sanctionui.dto.DocumentTypeOptionDto;
import com.impactsure.sanctionui.service.impl.CourseDocumentRequirementClientService;
import com.impactsure.sanctionui.service.impl.CourseFeeClient;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CourseDocumentRequirementUIController {

    private final CourseDocumentRequirementClientService requirementClientService;
    private final CourseFeeClient courseFeeClient;

    @GetMapping("/course-documents")
    public String listPage(Model model,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
            @AuthenticationPrincipal OidcUser oidcUser) {
        String accessToken = client.getAccessToken().getTokenValue();
        List<CourseFeeRequestDto> courses = courseFeeClient.getAllCoursesWithFee(accessToken);
        List<DocumentTypeOptionDto> docTypes = requirementClientService.listDocumentTypes(accessToken).stream()
                .filter(dt -> dt.getCode() == null || !dt.getCode().toUpperCase().contains("OTHER"))
                .toList();
        model.addAttribute("courses", courses);
        model.addAttribute("docTypes", docTypes);
        model.addAttribute("active", "course-documents");
        return "course-document-requirements";
    }

    @GetMapping("/course-documents/master")
    public ResponseEntity<List<CourseDocumentRequirementDto>> list(
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client) {
        String accessToken = client.getAccessToken().getTokenValue();
        return ResponseEntity.ok(requirementClientService.listRequirements(accessToken));
    }

    @PostMapping("/course-documents/master")
    public ResponseEntity<CourseDocumentRequirementDto> create(
            @RequestBody CourseDocumentRequirementRequest req,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
            @AuthenticationPrincipal OidcUser oidcUser,
            @ModelAttribute("permissionSet") Set<String> permissionSet) {
        ensureCanEdit(oidcUser, permissionSet, "course-documents:EDIT");
        String accessToken = client.getAccessToken().getTokenValue();
        CourseDocumentRequirementDto created = requirementClientService.createRequirement(req, accessToken);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/course-documents/master/{id}")
    public ResponseEntity<CourseDocumentRequirementDto> update(
            @PathVariable Long id,
            @RequestBody CourseDocumentRequirementRequest req,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
            @AuthenticationPrincipal OidcUser oidcUser,
            @ModelAttribute("permissionSet") Set<String> permissionSet) {
        ensureCanEdit(oidcUser, permissionSet, "course-documents:EDIT");
        String accessToken = client.getAccessToken().getTokenValue();
        CourseDocumentRequirementDto updated = requirementClientService.updateRequirement(id, req, accessToken);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/course-documents/master/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
            @AuthenticationPrincipal OidcUser oidcUser,
            @ModelAttribute("permissionSet") Set<String> permissionSet) {
        ensureCanEdit(oidcUser, permissionSet, "course-documents:EDIT");
        String accessToken = client.getAccessToken().getTokenValue();
        requirementClientService.deleteRequirement(id, accessToken);
        return ResponseEntity.noContent().build();
    }

    private void ensureCanEdit(OidcUser oidcUser, Set<String> permissionSet, String requiredPermission) {
        if (isSuperAdmin(oidcUser)) {
            return;
        }
        if (permissionSet != null && permissionSet.contains(requiredPermission)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "SUPER_ADMIN role required");
    }

    private boolean isSuperAdmin(OidcUser oidcUser) {
        if (oidcUser == null) {
            return false;
        }
        return oidcUser.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a));
    }
}
