package com.impactsure.sanctionui.web;

import com.impactsure.sanctionui.entities.PaymentModeMaster;
import com.impactsure.sanctionui.service.impl.PaymentModeApiClientService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Controller
@RequestMapping("/payment-modes")
public class PaymentModeMasterController {

	@Autowired
    private  PaymentModeApiClientService paymentModeApiClientService;


    // 🔹 Show list + create/edit form
    @GetMapping
    public String listPage(Model model,
                           @RequestParam(value = "id", required = false) Long id,
                           @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
               	        @AuthenticationPrincipal OidcUser oidcUser
               	        ) {
               		
               		 String accessToken = client.getAccessToken().getTokenValue();

        // 👉 Fetch from backend via API client
        List<PaymentModeMaster> modes = paymentModeApiClientService.findAllSorted(accessToken);
        model.addAttribute("paymentModes", modes);

        PaymentModeMaster form;
        if (id != null) {
            form = paymentModeApiClientService.findById(id,accessToken)
                    .orElseGet(PaymentModeMaster::new);
        } else {
            form = new PaymentModeMaster();
            form.setActive(true);   // default active
        }
        model.addAttribute("paymentMode", form);
        model.addAttribute("active", "paymentmodes");
        return "payment-modes";
    }

    // 🔹 Create / Update
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("paymentMode") PaymentModeMaster paymentMode,
                       BindingResult bindingResult,
                       Model model,@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
              	        @AuthenticationPrincipal OidcUser oidcUser
              	        ) {
              		
              		 String accessToken = client.getAccessToken().getTokenValue();

        if (bindingResult.hasErrors()) {
            // Reload list in case of validation errors
            List<PaymentModeMaster> modes = paymentModeApiClientService.findAllSorted(accessToken);
            model.addAttribute("paymentModes", modes);
            return "payment-modes";
        }

        // 👉 Persist via API client
        paymentModeApiClientService.save(paymentMode,accessToken);
        return "redirect:/payment-modes";
    }

    // 🔹 Edit (just redirects with id)
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id) {
        return "redirect:/payment-modes?id=" + id;
    }

    // 🔹 Delete
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id,@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
  	        @AuthenticationPrincipal OidcUser oidcUser
  	        ) {
  		
  		 String accessToken = client.getAccessToken().getTokenValue();
        paymentModeApiClientService.deleteById(id,accessToken);
        return "redirect:/payment-modes";
    }
}
