package com.impactsure.sanctionui.web;

import com.impactsure.sanctionui.dto.CancelAdmissionDTO;
import lombok.RequiredArgsConstructor;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.impactsure.sanctionui.dto.AdmissionDto;
import com.impactsure.sanctionui.dto.PaymentModeDto;
import com.impactsure.sanctionui.entities.Admission2;
import com.impactsure.sanctionui.entities.Course;
import com.impactsure.sanctionui.entities.FileUpload;
import com.impactsure.sanctionui.entities.Guardian;
import com.impactsure.sanctionui.entities.YearlyFees;
import com.impactsure.sanctionui.enums.AdmissionStatus;
import com.impactsure.sanctionui.enums.GuardianRelation;
import com.impactsure.sanctionui.repository.CourseRepository;
import com.impactsure.sanctionui.repository.YearlyFeesRepository;
import com.impactsure.sanctionui.service.impl.AdmissionApiClientService;
import com.impactsure.sanctionui.service.impl.PaymentModeApiClientService;
import com.nimbusds.jwt.SignedJWT;

import ch.qos.logback.core.model.Model;



@Controller
@RequiredArgsConstructor
public class AdmissionFormController {
	
	@Autowired
	private CourseRepository courseRepository;
	
	@Autowired
	private AdmissionApiClientService admissionApiClientService;
	
	@Autowired
	private YearlyFeesRepository yearlyFeesRepository;
	
	@Autowired
	private PaymentModeApiClientService paymentModeApiClientService;


	public List<String> clientRoleNames(OidcUser user){
		return user.getAuthorities().stream()
			      .map(a -> a.getAuthority())
			      .filter(a -> a.startsWith("ROLE_"))
			      .map(a -> a.substring("ROLE_".length()))
			      .toList();
	    	}
	
	public String getSingleRole(List<String> roles) {
		String role="";
		
		if(roles.contains("ADMIN")) {
			role="ADMIN";
		}else if(roles.contains("BRANCH_USER")) {
			role="BRANCH_USER";
		}else if(roles.contains("HO")) {
			role="HO";
		}
		return role;	 
	}
	
	@GetMapping("/landing")
	public String test() {
		
		return "landing";
	}
	@GetMapping("/newadmission")
	public ModelAndView admission(@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
	        @AuthenticationPrincipal OidcUser oidcUser
	        ) {
		
		 String accessToken = client.getAccessToken().getTokenValue();
		ModelAndView model =  new ModelAndView();
		List<Course> courses = this.courseRepository.findAll();
		
		List<PaymentModeDto> paymentModes = paymentModeApiClientService.getPaymentModes(accessToken);
		 List<String> paymentModeStrings = new ArrayList<>();
	        for(PaymentModeDto  mode:paymentModes) {
	        	paymentModeStrings.add(mode.getCode());
	        }
		model.addObject("courses",courses);
		model.addObject("paymentModes", paymentModeStrings);
		model.setViewName("admission-from");
		
		return model;
	}
	
	@GetMapping("/admissionlist")
	public ModelAndView listAdmissions(
	    @RequestParam(defaultValue = "") String q,
	    @RequestParam(defaultValue = "PENDING,ADMITTED,UNDER_CANCELLATION,CANCELLED") String status,
	    @RequestParam(defaultValue = "0") int page,
	    @RequestParam(defaultValue = "25") int size,
	    @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
        @AuthenticationPrincipal OidcUser oidcUser
	    
	){
		ModelAndView model = new ModelAndView();
		
		if(status.isEmpty()) {
			status = "PENDING,ADMITTED";
		}
	    // Parse statuses
	    List<AdmissionStatus> statuses = Arrays.stream(status.split(","))
	        .map(String::trim)
	        .filter(s -> !s.isEmpty())
	        .map(AdmissionStatus::valueOf) // enum: PENDING, SUCCESS, etc.
	        .toList();

	    Pageable pageable = PageRequest.of(Math.max(page,0), Math.min(size,100), Sort.by(Sort.Direction.DESC,"createdAt"));

//	    Page<AdmissionDto> result = admissionsService.search(q, statuses, pageable);

	    Page<Admission2> result = admissionApiClientService.searchAdmissions(q, status, page, size);
	    
	    
	    List<String> roles = clientRoleNames(oidcUser);
	    String role = getSingleRole(roles);
	    // Add roles to your model (or use them for access control)
	    model.addObject("role", role);
		    
	    model.addObject("page", result);
	    model.addObject("q", q);
	    model.addObject("size", size);
	    model.addObject("statusSelected", status);
	    model.setViewName("admissions/admissions-list");
	    return model;
	}
	
	@GetMapping("/admissions")
	public ModelAndView viewAdmission(@RequestParam Long id,
			@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
	        @AuthenticationPrincipal OidcUser oidcUser) {
		ModelAndView model = new ModelAndView();
		String accessToken = client.getAccessToken().getTokenValue();

	    Admission2 admission = admissionApiClientService.getAdmissionById(id);
	    
	    Guardian father = admission.getStudent().getGuardians().stream()
	            .filter(g -> g.getRelation() == GuardianRelation.Father)
	            .findFirst().orElse(new Guardian());
        Guardian mother = admission.getStudent().getGuardians().stream()
            .filter(g -> g.getRelation() == GuardianRelation.Mother)
            .findFirst().orElse(new Guardian());
	    
        
        Map<Long, FileUpload> receipts = admissionApiClientService.findReceiptMapForAdmission(id);
        Map<String, FileUpload> docUploads = admissionApiClientService.findDocUploadMapForAdmission(id);
        
        List<YearlyFees> yearlyFees = this.yearlyFeesRepository.findByAdmissionAdmissionId(id);
        
        Map<Integer, Double> yearlyFeesMap = new HashMap<Integer, Double>(); 
        
        for(YearlyFees fees:yearlyFees)
        {
        	yearlyFeesMap.put(fees.getYear(), fees.getFees());
        }
        
        List<PaymentModeDto> paymentModes = paymentModeApiClientService.getPaymentModes(accessToken);
        List<String> paymentModeStrings = new ArrayList<>();
        for(PaymentModeDto  mode:paymentModes) {
        	paymentModeStrings.add(mode.getCode());
        }
        CancelAdmissionDTO cancelAdmissionDTO = admissionApiClientService.fetchCancelAdmissionDetails(id,accessToken);
        List<String> roles = clientRoleNames(oidcUser);
	    String role = getSingleRole(roles);
	    model.addObject("role", role);
        model.addObject("id", id);
        model.addObject("father", father);
        model.addObject("mother", mother);
	    model.addObject("admission", admission);
	    model.addObject("courses", courseRepository.findAll());
	    model.addObject("receipts", receipts);
	    model.addObject("docUploads", docUploads);
	    model.addObject("yearlyFees", yearlyFeesMap); 
		model.addObject("paymentModes", paymentModeStrings);
		model.addObject("cancellation", cancelAdmissionDTO);
	    model.setViewName("admissions/admission-view");
	    
	    model.addObject("hasExistingInstallments", !admission.getInstallments().isEmpty());
	    model.addObject("existingInstallments", admission.getInstallments());

	    return model; // -> templates/admissions/view.html
	}
	
	@GetMapping("/admissionsprint")
	public ModelAndView printAdmission(@RequestParam Long id,
	                                   @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
	                                   @AuthenticationPrincipal OidcUser oidcUser) {

	    ModelAndView model = new ModelAndView();

	    String accessToken = client.getAccessToken().getTokenValue();

	    Admission2 admission = admissionApiClientService.getAdmissionById(id);

	    Guardian father = admission.getStudent().getGuardians().stream()
	            .filter(g -> g.getRelation() == GuardianRelation.Father)
	            .findFirst()
	            .orElse(new Guardian());

	    Guardian mother = admission.getStudent().getGuardians().stream()
	            .filter(g -> g.getRelation() == GuardianRelation.Mother)
	            .findFirst()
	            .orElse(new Guardian());

	    // Year-wise fees (same as view)
	    List<YearlyFees> yearlyFees = yearlyFeesRepository.findByAdmissionAdmissionId(id);
	    Map<Integer, Double> yearlyFeesMap = new HashMap<>();
	    for (YearlyFees fees : yearlyFees) {
	        yearlyFeesMap.put(fees.getYear(), fees.getFees());
	    }

	    model.addObject("admission", admission);
	    model.addObject("father", father);
	    model.addObject("mother", mother);
	    model.addObject("yearlyFees", yearlyFeesMap);

	    model.setViewName("admissions/admission-print");
	    return model;
	}

}
