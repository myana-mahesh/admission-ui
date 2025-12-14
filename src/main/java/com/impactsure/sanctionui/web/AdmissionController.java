package com.impactsure.sanctionui.web;

import com.impactsure.sanctionui.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.impactsure.sanctionui.entities.Admission2;
import com.impactsure.sanctionui.entities.Student;
import com.impactsure.sanctionui.service.impl.AdmissionApiClientService;
import com.impactsure.sanctionui.service.impl.StudentApiClientService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;;


@Controller
@RequestMapping("/admission")
@Slf4j
public class AdmissionController {
	
	@Autowired
	private StudentApiClientService studentApiClientService;
	
	@Autowired
	private  AdmissionApiClientService admissionApiClientService;

	@Value("${upload.base-dir}")
	private String uploadBaseDir;

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

	@PostMapping("/cancel-admission")
	public ResponseEntity<String> cancelAdmission(
			@RequestBody CancelAdmissionDTO dto,
			@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
			@AuthenticationPrincipal OidcUser oidcUser
	) {

		String accessToken = client.getAccessToken().getTokenValue();

		String resp = this.admissionApiClientService.cancelAdmission(dto, accessToken);

		if (resp != null) {
			return new ResponseEntity<>(resp, HttpStatus.OK);
		}

		return new ResponseEntity<>("Something went wrong!", HttpStatus.INTERNAL_SERVER_ERROR);
	}



	@PostMapping(
			value = "/save-cancellation-details",
			consumes = MediaType.MULTIPART_FORM_DATA_VALUE
	)
	public ResponseEntity<?> saveCancellationDetails(
			@RequestParam Long admissionId,
			@RequestParam Double cancelCharges,
			@RequestParam String handlingPerson,
			@RequestParam String remark,
			@RequestParam(required = false) String refundProofFileName,
			@RequestPart(required = false) MultipartFile refundProof,
			@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
			@AuthenticationPrincipal OidcUser oidcUser
	) {
		String accessToken = client.getAccessToken().getTokenValue();

		// 🔹 FILE SAVE
		if (refundProof != null && !refundProof.isEmpty()) {


			// create directory if not exists
			Path admissionDir = Paths.get(uploadBaseDir +"//cancellation_details_docs", String.valueOf(admissionId));
			try {
				Files.createDirectories(admissionDir);

				Path targetFile = admissionDir.resolve(Objects.requireNonNull(refundProof.getOriginalFilename()));
				Files.copy(refundProof.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
				refundProofFileName=targetFile.getFileName().toString();
			} catch (IOException e) {
				throw new RuntimeException("File upload failed", e);
			}
		}

		this.admissionApiClientService.cancelAdmissionDetailsUpdate(
				admissionId,
				cancelCharges,
				handlingPerson,
				remark,
				refundProofFileName,
				accessToken
		);

		return ResponseEntity.ok("Cancellation details saved successfully");
	}

	@GetMapping("/download-cancellation-proof/{admissionId}/{fileName}")
	public ResponseEntity<Resource> downloadCancellationProof(
			@PathVariable Long admissionId,
			@PathVariable String fileName
	) throws IOException {

		Path filePath = Paths.get(uploadBaseDir+"//cancellation_details_docs",
				String.valueOf(admissionId),
				fileName);

		if (!Files.exists(filePath)) {
			return ResponseEntity.notFound().build();
		}

		Resource resource = new UrlResource(filePath.toUri());

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION,
						"attachment; filename=\"" + fileName + "\"")
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(resource);
	}

}
