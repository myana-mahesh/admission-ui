package com.impactsure.sanctionui.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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
}

