package com.impactsure.sanctionui.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import com.impactsure.sanctionui.dto.CollegeDto;
import com.impactsure.sanctionui.dto.CollegeForm;
import com.impactsure.sanctionui.dto.CollegeCourseSeatDto;
import com.impactsure.sanctionui.dto.CourseFeeRequestDto;
import com.impactsure.sanctionui.service.impl.CollegeApiClientService;
import com.impactsure.sanctionui.service.impl.CourseFeeClient;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("")
@RequiredArgsConstructor
public class CollegeMasterController {

    private final CollegeApiClientService collegeApiClientService;
    private final CourseFeeClient courseFeeClient;

    @GetMapping("/colleges")
    public String listColleges(Model model,
                               @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
                               @AuthenticationPrincipal OidcUser oidcUser) {
        String accessToken = client.getAccessToken().getTokenValue();

        List<CollegeDto> colleges = collegeApiClientService.listAll(accessToken);
        Map<Long, List<CollegeCourseSeatDto>> collegeSeatMap = new HashMap<>();
        for (CollegeDto college : colleges) {
            if (college.getCollegeId() == null) {
                continue;
            }
            List<CollegeCourseSeatDto> seats =
                    collegeApiClientService.getCollegeCourseSeats(college.getCollegeId(), accessToken);
            collegeSeatMap.put(college.getCollegeId(), seats);
        }
        model.addAttribute("colleges", colleges);
        model.addAttribute("collegeSeatMap", collegeSeatMap);
        model.addAttribute("active", "colleges");
        return "college-list";
    }

    @GetMapping("/collegesnew")
    public String createCollegeForm(Model model,
                                    @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
                                    @AuthenticationPrincipal OidcUser oidcUser) {
        return loadForm(null, model, client);
    }

    @GetMapping("/collegesedit")
    public String editCollegeForm(@RequestParam Long collegeId,
                                  Model model,
                                  @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
                                  @AuthenticationPrincipal OidcUser oidcUser) {
        return loadForm(collegeId, model, client);
    }

    @PostMapping("/collegessave")
    public String saveCollege(@ModelAttribute("collegeForm") CollegeForm form,
                              @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
                              @AuthenticationPrincipal OidcUser oidcUser) {
        String accessToken = client.getAccessToken().getTokenValue();

        List<CollegeDto.CollegeCourseDto> courseDtos = new ArrayList<>();
        if (form.getCourseIds() != null) {
            for (int i = 0; i < form.getCourseIds().size(); i++) {
                Long courseId = form.getCourseIds().get(i);
                if (courseId == null) {
                    continue;
                }
                Integer seats = 0;
                if (form.getTotalSeats() != null && form.getTotalSeats().size() > i) {
                    seats = form.getTotalSeats().get(i);
                }
                courseDtos.add(CollegeDto.CollegeCourseDto.builder()
                        .courseId(courseId)
                        .totalSeats(seats == null ? 0 : seats)
                        .build());
            }
        }

        CollegeDto dto = CollegeDto.builder()
                .collegeId(form.getCollegeId())
                .code(form.getCode())
                .name(form.getName())
                .courses(courseDtos)
                .build();

        CollegeDto saved = collegeApiClientService.save(dto, accessToken);
        return "redirect:/collegesedit?collegeId=" + saved.getCollegeId();
    }

    @PostMapping("/collegesdelete/{collegeId}")
    public String deleteCollege(@PathVariable Long collegeId,
                                @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
                                @AuthenticationPrincipal OidcUser oidcUser) {
        String accessToken = client.getAccessToken().getTokenValue();
        collegeApiClientService.delete(collegeId, accessToken);
        return "redirect:/colleges";
    }

    @DeleteMapping("/colleges/{collegeId}")
    @ResponseBody
    public ResponseEntity<String> deleteCollegeAjax(@PathVariable Long collegeId,
                                                    @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
                                                    @AuthenticationPrincipal OidcUser oidcUser) {
        String accessToken = client.getAccessToken().getTokenValue();
        return collegeApiClientService.delete(collegeId, accessToken);
    }

    private String loadForm(Long collegeId, Model model, OAuth2AuthorizedClient client) {
        String accessToken = client.getAccessToken().getTokenValue();

        List<CourseFeeRequestDto> courses = courseFeeClient.getAllCoursesWithFee(accessToken);
        model.addAttribute("courseOptions", courses);

        List<CollegeCourseSeatDto> seatSummary = new ArrayList<>();
        CollegeForm form = new CollegeForm();
        if (collegeId != null) {
            collegeApiClientService.getById(collegeId, accessToken).ifPresent(dto -> {
                form.setCollegeId(dto.getCollegeId());
                form.setCode(dto.getCode());
                form.setName(dto.getName());
                if (dto.getCourses() != null) {
                    dto.getCourses().forEach(cc -> {
                        form.getCourseIds().add(cc.getCourseId());
                        form.getTotalSeats().add(cc.getTotalSeats());
                    });
                }
            });
            seatSummary = collegeApiClientService.getCollegeCourseSeats(collegeId, accessToken);
        }

        if (form.getCourseIds().isEmpty()) {
            form.getCourseIds().add(null);
            form.getTotalSeats().add(0);
        }

        model.addAttribute("collegeForm", form);
        model.addAttribute("collegeSeatMap", seatSummary);
        model.addAttribute("active", "colleges");
        return "college-form";
    }
}
