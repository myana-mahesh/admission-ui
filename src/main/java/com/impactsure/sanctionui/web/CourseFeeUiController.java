package com.impactsure.sanctionui.web;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import com.impactsure.sanctionui.dto.CourseFeeForm;
import com.impactsure.sanctionui.dto.CourseFeeRequestDto;
import com.impactsure.sanctionui.service.impl.CourseFeeClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("")
@RequiredArgsConstructor
public class CourseFeeUiController {

    private final CourseFeeClient courseFeeClient;

    // Show create form
    @GetMapping("/coursescreate")
    public String showCreateForm(Model model) {
        CourseFeeForm form = new CourseFeeForm();
        // One empty row by default
        form.getInstallmentIds().add(null);
        form.getInstallmentSequences().add(1);
        form.getInstallmentAmounts().add(BigDecimal.ZERO);
        form.getInstallmentDueDays().add(0);
        form.getInstallmentYears().add(1); // default Year 1


        model.addAttribute("courseForm", form);
        return "course-fee-form"; // templates/course-fee-form.html
    }

    // Show edit form
    @GetMapping("/courseedit")
    public String showEditForm(@RequestParam Long courseId, Model model,@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
 	        @AuthenticationPrincipal OidcUser oidcUser
 	        ) {
 		
 		 String accessToken = client.getAccessToken().getTokenValue();
        CourseFeeRequestDto dto = courseFeeClient.getCourseWithFee(courseId,accessToken);

        CourseFeeForm form = new CourseFeeForm();
        form.setCourseId(dto.getCourseId());
        form.setCode(dto.getCode());
        form.setName(dto.getName());
        form.setYears(dto.getYears());

        if (dto.getFeeTemplate() != null) {
            form.setTemplateId(dto.getFeeTemplate().getId());
            form.setTemplateName(dto.getFeeTemplate().getName());

            if (!CollectionUtils.isEmpty(dto.getFeeTemplate().getInstallments())) {
            	dto.getFeeTemplate().getInstallments().forEach(inst -> {
            	    form.getInstallmentIds().add(inst.getId());
            	    form.getInstallmentSequences().add(inst.getSequence());
            	    form.getInstallmentAmounts().add(inst.getAmount());
            	    form.getInstallmentDueDays().add(inst.getDueDayOfMonth());
            	    form.getInstallmentDueMonths().add(inst.getDueMonth());
            	    form.getInstallmentYears().add(inst.getYearNumber() != null ? inst.getYearNumber() : 1); 
            	});

            }
        }

        // Ensure at least one row
        if (form.getInstallmentIds().isEmpty()) {
        	form.getInstallmentIds().add(null);
        	form.getInstallmentSequences().add(1);
        	form.getInstallmentAmounts().add(BigDecimal.ZERO);
        	form.getInstallmentDueDays().add(0);
        	form.getInstallmentYears().add(1); // default Year 1

        }

        model.addAttribute("courseForm", form);
        return "course-fee-form";
    }

    // Handle save (create or update)
    @PostMapping("/coursessave")
    public String saveCourse(@ModelAttribute("courseForm") CourseFeeForm form,
                             Model model,@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
                 	        @AuthenticationPrincipal OidcUser oidcUser
                 	        ) {
                 		
                 		 String accessToken = client.getAccessToken().getTokenValue();

        // Build DTO to send to Admission Service
        List<CourseFeeRequestDto.InstallmentDto> installments = new ArrayList<>();

        for (int i = 0; i < form.getInstallmentAmounts().size(); i++) {
            BigDecimal amount = form.getInstallmentAmounts().get(i);
            if (amount == null) continue;
            if (amount.compareTo(BigDecimal.ZERO) <= 0) continue;

            Integer yearNumber = 1;
            if (form.getInstallmentYears() != null && form.getInstallmentYears().size() > i) {
                yearNumber = form.getInstallmentYears().get(i);
                if (yearNumber == null || yearNumber <= 0) yearNumber = 1;
            }

            CourseFeeRequestDto.InstallmentDto inst = CourseFeeRequestDto.InstallmentDto.builder()
                    .id(form.getInstallmentIds().get(i))
                    .sequence(form.getInstallmentSequences().get(i))
                    .amount(amount)
                    .dueDaysFromAdmission(form.getInstallmentDueDays().get(i))
                    .dueDayOfMonth(form.getInstallmentDueDays().get(i))
                    .dueMonth(form.getInstallmentDueMonths().get(i))
                    .yearNumber(yearNumber)       
                    .build();

            installments.add(inst);
        }


        BigDecimal total = installments.stream()
                .map(CourseFeeRequestDto.InstallmentDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CourseFeeRequestDto.FeeTemplateDto feeTemplate =
                CourseFeeRequestDto.FeeTemplateDto.builder()
                        .id(form.getTemplateId())
                        .name(form.getTemplateName())
                        .totalAmount(total)
                        .installments(installments)
                        .build();

        CourseFeeRequestDto dto = CourseFeeRequestDto.builder()
                .courseId(form.getCourseId())
                .code(form.getCode())
                .name(form.getName())
                .years(form.getYears())
                .feeTemplate(feeTemplate)
                .build();

        CourseFeeRequestDto saved;
        if (form.getCourseId() == null) {
            saved = courseFeeClient.createCourseWithFee(dto,accessToken);
        } else {
            saved = courseFeeClient.updateCourseWithFee(form.getCourseId(), dto,accessToken);
        }

        // After save, redirect to edit page or list page
        return "redirect:/courseedit?courseId="+saved.getCourseId();
        
    }
    
    @GetMapping("/course-fee")
    @ResponseBody
    public CourseFeeRequestDto getCourseFee(@RequestParam Long courseId,@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
 	        @AuthenticationPrincipal OidcUser oidcUser
 	        ) {
 		
 		 String accessToken = client.getAccessToken().getTokenValue();
        return courseFeeClient.getCourseWithFee(courseId, accessToken);
    }
    
    @GetMapping("/courses")
    public String listCourses(Model model,
                              @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
                              @AuthenticationPrincipal OidcUser oidcUser) {

        String accessToken = client.getAccessToken().getTokenValue();

        // 🔹 You can either:
        // 1) Reuse CourseFeeRequestDto as list item
        // 2) Or create a lightweight DTO if you prefer
        //
        // This assumes you add a method in CourseFeeClient like:
        // List<CourseFeeRequestDto> getAllCoursesWithFee(String accessToken);

        List<CourseFeeRequestDto> courses =
                courseFeeClient.getAllCoursesWithFee(accessToken);

        model.addAttribute("courses", courses);
        return "course-list";   // -> templates/course-fee-list.html
    }
}
