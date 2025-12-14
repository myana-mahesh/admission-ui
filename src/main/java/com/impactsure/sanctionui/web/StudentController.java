package com.impactsure.sanctionui.web;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import com.impactsure.sanctionui.dto.AdmissionRequestFromUI;
import com.impactsure.sanctionui.dto.CreateAdmissionRequest;
import com.impactsure.sanctionui.dto.CreateStudentRequest;
import com.impactsure.sanctionui.dto.GuardianDto;
import com.impactsure.sanctionui.dto.OfficeUpdateRequest;
import com.impactsure.sanctionui.dto.PagedResponse;
import com.impactsure.sanctionui.dto.StudentDto;
import com.impactsure.sanctionui.dto.StudentPerkDto;
import com.impactsure.sanctionui.dto.StudentPerksMasterDto;
import com.impactsure.sanctionui.entities.Admission2;
import com.impactsure.sanctionui.entities.Student;
import com.impactsure.sanctionui.service.impl.StudentApiClientService;
import com.impactsure.sanctionui.service.impl.StudentPerkClientService;
import com.impactsure.sanctionui.service.impl.StudentPerksMasterService;

import lombok.extern.slf4j.Slf4j;


@Controller
@Slf4j
public class StudentController {
	
	@Autowired
	private StudentApiClientService studentApiClientService;
	
	@Autowired
	private StudentPerksMasterService studentPerksMasterService;
	
	@Autowired
	private StudentPerkClientService studentPerkClientService;
	
	@GetMapping("/studentpage")
	  public ResponseEntity<PagedResponse<StudentDto> > list(
	      @RequestParam(defaultValue = "0") int page,
	      @RequestParam(defaultValue = "10") int size,
	      @RequestParam(required = false) String q,
	      @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
	        @AuthenticationPrincipal OidcUser oidcUser
	        ) {
		
		 String accessToken = client.getAccessToken().getTokenValue();
	      // 0-based page index, sorted by createdAt desc (from Auditable)
	      Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
	      PagedResponse<StudentDto> results = studentApiClientService.getStudents(page, size, q, accessToken);
	   
	      return new ResponseEntity<PagedResponse<StudentDto>>(results,HttpStatus.OK);
	  }
	
	@GetMapping("/studentlist")
    public String listStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String q,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
            @AuthenticationPrincipal OidcUser oidcUser,
            Model model
    ) {
        String accessToken = client.getAccessToken().getTokenValue();

        // Call admission-service via RestTemplate
        PagedResponse<StudentDto> studentsPage =
                studentApiClientService.getStudents(page, size, q, accessToken);

        model.addAttribute("page", studentsPage);  // same name as in admissionlist
        model.addAttribute("q", q);
        model.addAttribute("size", size);

        return "studentlist"; // -> src/main/resources/templates/studentlist.html
    }
	
	@GetMapping("/students")
    public String viewStudent(
            @RequestParam Long id,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
            @AuthenticationPrincipal OidcUser oidcUser,
            Model model
    ) {
        String accessToken = client.getAccessToken().getTokenValue();

        StudentDto student = studentApiClientService.getStudentById(id, accessToken);
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");
        }

        GuardianDto father = null;
        GuardianDto mother = null;
        if (student.getGuardians() != null) {
            for (GuardianDto g : student.getGuardians()) {
                if ("Father".equalsIgnoreCase(g.getRelation())) {
                    father = g;
                } else if ("Mother".equalsIgnoreCase(g.getRelation())) {
                    mother = g;
                }
            }
        }

        model.addAttribute("student", student);
        model.addAttribute("father", father);
        model.addAttribute("mother", mother);
        
        List<StudentPerksMasterDto> perks = this.studentPerksMasterService.getAllPerks(accessToken);
        model.addAttribute("perksList", perks);
        
       List<StudentPerkDto> studentPerks  =  this.studentPerkClientService.getPerksForStudent(student.getStudentId(),accessToken);
       List<Long> studentPerkIds = extractPerkIds(studentPerks);
       model.addAttribute("studentPerkIds", studentPerkIds);
        return "studentview"; // -> src/main/resources/templates/studentview.html
    }
	
	public List<Long> extractPerkIds(List<StudentPerkDto> perks) {
	    if (perks == null) return List.of(); // avoid NPE
	    return perks.stream()
	            .map(StudentPerkDto::getPerkId)
	            .collect(Collectors.toList());
	}
	
	@PostMapping("/student/update")
	@ResponseBody
	public ResponseEntity<?> createStudent(@RequestBody AdmissionRequestFromUI admissionReq,
		
			@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
	        @AuthenticationPrincipal OidcUser oidcUser
	        ) {
		
		 String accessToken = client.getAccessToken().getTokenValue();

		Student student = null;
		Long studenId = null;
		try {
			if(admissionReq.getStudentId()!=null) {
				studenId = Long.parseLong(admissionReq.getStudentId());
			}
		} catch (Exception e) {
			log.error("error converting studentId to long");
		}
		try {
			CreateStudentRequest createReq = CreateStudentRequest.builder()
				    .fullName(admissionReq.getFullName())
				    .dob(admissionReq.getDob())
				    .gender(admissionReq.getGender())
				    .aadhaar(admissionReq.getAadhaar())
				    .email(admissionReq.getEmail())
				    .nationality(admissionReq.getNationality())
				    .religion(admissionReq.getReligion())
				    .caste(admissionReq.getCaste())
				    .mobile(admissionReq.getMobile())
				    .absId(admissionReq.getAbsId())
				    .addressLine1(admissionReq.getAddressLine1())
				    .city(admissionReq.getCity())
				    .state(admissionReq.getState())
				    .pincode(admissionReq.getPincode())
				    .fatherName(admissionReq.getFatherName())
				    .fatherMobile(admissionReq.getFatherMobile())
				    .motherName(admissionReq.getMotherName())
				    .motherMobile(admissionReq.getMotherMobile())
				    .course(admissionReq.getCourse())
				    .studendId(studenId)
				    .build();

			student = this.studentApiClientService.createOrUpdateStudent(createReq,accessToken);
			return new ResponseEntity<Student>(student,HttpStatus.OK);
			
		}catch (Exception e) {
			log.error("error calling student create API");
			e.printStackTrace();
		}
		

		return new ResponseEntity<Student>(student,HttpStatus.INTERNAL_SERVER_ERROR);
	}

}
