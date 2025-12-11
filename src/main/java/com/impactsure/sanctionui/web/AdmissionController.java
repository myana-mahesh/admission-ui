package com.impactsure.sanctionui.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.impactsure.sanctionui.dto.AdmissionRequestFromUI;
import com.impactsure.sanctionui.dto.CreateAdmissionRequest;
import com.impactsure.sanctionui.dto.CreateStudentRequest;
import com.impactsure.sanctionui.dto.OfficeUpdateRequest;
import com.impactsure.sanctionui.entities.Admission2;
import com.impactsure.sanctionui.entities.Student;
import com.impactsure.sanctionui.service.impl.AdmissionApiClientService;
import com.impactsure.sanctionui.service.impl.StudentApiClientService;

import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/admission")
@Slf4j
public class AdmissionController {
	
	@Autowired
	private StudentApiClientService studentApiClientService;
	
	@Autowired
	private  AdmissionApiClientService admissionApiClientService;

	
	@PostMapping("/student/create")
	@ResponseBody
	public ResponseEntity<?> postMethodName(@RequestBody AdmissionRequestFromUI admissionReq,
		
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
			
			try {
				OfficeUpdateRequest officeReq = OfficeUpdateRequest.builder()
					    .lastCollege(admissionReq.getLastCollege())
					    .collegeAttended(admissionReq.getCollegeAttended())
					    .collegeLocation(admissionReq.getCollegeLocation())
					    .remarks(admissionReq.getRemarks())
					    .examDueDate(admissionReq.getExamDueDate())
					    .dateOfAdmission(admissionReq.getDateOfAdmission())
					    .build();
				
				CreateAdmissionRequest createAdmissionReq = CreateAdmissionRequest.builder()
						.courseCode(admissionReq.getCourse())
						.studentId(student.getStudentId())
						.academicYearLabel("2025-2026")
						.officeUpdateRequest(officeReq)
						.totalFees(admissionReq.getTotalFees())
						.discount(admissionReq.getDiscountAmount())
						.noOfInstallments(admissionReq.getInstallmentsCount())
						.formNo(admissionReq.getFormNo())
						.formDate(admissionReq.getFormDate())
						.build();
				
				Admission2 result = admissionApiClientService.createAdmission(createAdmissionReq,accessToken);
		        return ResponseEntity.ok(result);
			} catch (Exception e) {
				// TODO: handle exception
			}
			
		}catch (Exception e) {
			log.error("error calling student create API");
			e.printStackTrace();
		}
		

		return new ResponseEntity<Student>(student,HttpStatus.INTERNAL_SERVER_ERROR);
	}
	
	
	@PostMapping("/send-acknowledgement")
	public ResponseEntity<Admission2> sendAcknowledgement(@RequestParam Long id,@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
	        @AuthenticationPrincipal OidcUser oidcUser
	        ) {
		
		 String accessToken = client.getAccessToken().getTokenValue();
		
		Admission2 admisison = this.admissionApiClientService.acknowledgeAdmission(id,accessToken);
		if(admisison!=null) {
			return new ResponseEntity<Admission2>(admisison, HttpStatus.OK);
		}

		return new ResponseEntity<Admission2>(admisison, HttpStatus.INTERNAL_SERVER_ERROR);
	}
	


}
