package com.impactsure.sanctionui.web;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.impactsure.sanctionui.dto.FeeInstallmentInvoiceResponse;
import com.impactsure.sanctionui.service.impl.FeeInstallmentUiClient;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/api/installments")
public class FeeInstallmentUiController {

	@Autowired
    private FeeInstallmentUiClient feeInstallmentUiClient;

    @PostMapping("/{installmentId}/status")
    @ResponseBody
    public FeeInstallmentInvoiceResponse updateInstallmentStatusFromUi(
            @PathVariable Long installmentId,
            @RequestParam String status
            ,@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
	        @AuthenticationPrincipal OidcUser oidcUser
	        ) {
		
		 String accessToken = client.getAccessToken().getTokenValue();  // adjust per your security setup
        return feeInstallmentUiClient.updateInstallmentStatus(installmentId, status, accessToken);
    }
    
    @ResponseBody
    @DeleteMapping("/{installmentId}/delete")
    public String deleteInstallmentFromForm(
            @PathVariable Long installmentId,
            @RequestParam(defaultValue = "false") boolean deleteFilesAlso,
            @RequestParam(required = false) Long admissionId,  // so we can redirect back
            @RequestHeader(value = "Authorization", required = false) String authorization,
            RedirectAttributes ra,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
	        @AuthenticationPrincipal OidcUser oidcUser
	        ) {
		
		 String accessToken = client.getAccessToken().getTokenValue();

        try {
            feeInstallmentUiClient.deleteInstallment(installmentId, deleteFilesAlso, accessToken);
            ra.addFlashAttribute("success", "Installment deleted.");
            return "Deleted";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Delete failed: " + e.getMessage());
        }

        return null;
    }
}

