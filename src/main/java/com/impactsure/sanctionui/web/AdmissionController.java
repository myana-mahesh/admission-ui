package com.impactsure.sanctionui.web;

import com.impactsure.sanctionui.dto.*;
import com.impactsure.sanctionui.service.impl.StudentDocumentVerificationService;
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
import com.impactsure.sanctionui.service.impl.FileStorageService;
import com.impactsure.sanctionui.service.impl.StudentApiClientService;
import com.impactsure.sanctionui.utils.HashUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	@Autowired
	private  StudentDocumentVerificationService documentService;

	@Autowired
	private FileStorageService fileStorageService;

	@Value("${upload.base-dir}")
	private String uploadBaseDir;
	
	@Value("${cancel.base-dir}")
	private String cancelBaseDir;

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
				    .area(admissionReq.getArea())
				    .city(admissionReq.getCity())
				    .state(admissionReq.getState())
				    .pincode(admissionReq.getPincode())
				    .fatherName(admissionReq.getFatherName())
				    .fatherMobile(admissionReq.getFatherMobile())
				    .motherName(admissionReq.getMotherName())
				    .motherMobile(admissionReq.getMotherMobile())
				    .course(admissionReq.getCourse())
				    .courseCode(admissionReq.getCourse())
					.bloodGroup(admissionReq.getBloodGroup())
				    .studendId(studenId)
					.sscDetails(admissionReq.getSscDetails())
					.hscDetails(admissionReq.getHscDetails())
					.batch(admissionReq.getBatch())
					.registrationNumber(admissionReq.getRegistrationNumber())
					.age(admissionReq.getAge())
					.otherPayments(admissionReq.getOtherPayments())
					//.courseCode(admissionReq.getCourse())
					///.academicYearLabel("2025-2026")
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
						.batch(admissionReq.getBatch())
						.registrationNumber(admissionReq.getRegistrationNumber())
						.referenceName(admissionReq.getReferenceName())
					    .build();
				
				CreateAdmissionRequest createAdmissionReq = CreateAdmissionRequest.builder()
						.courseCode(admissionReq.getCourse())
						.collegeId(admissionReq.getCollegeId())
						.studentId(student.getStudentId())
						.academicYearLabel("2025-2026")
						.officeUpdateRequest(officeReq)
						.totalFees(admissionReq.getTotalFees())
						.discount(admissionReq.getDiscountAmount())
						.discountRemark(admissionReq.getDiscountRemark())
						.discountRemarkOther(admissionReq.getDiscountRemarkOther())
						.noOfInstallments(admissionReq.getInstallmentsCount())
						.formNo(admissionReq.getFormNo())
						.formDate(admissionReq.getFormDate())
						.admissionBranchId(admissionReq.getAdmissionBranchId())
						.lectureBranchId(admissionReq.getLectureBranchId())

						.build();
				
				Admission2 result = admissionApiClientService.createAdmission(createAdmissionReq,accessToken);
		        return ResponseEntity.ok(Map.of(
		        		"admissionId", result != null ? result.getAdmissionId() : null,
		        		"studentId", student != null ? student.getStudentId() : null
		        ));
			} catch (Exception e) {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body(Map.of("message", toUserMessage(e, admissionReq)));
			}
			
		}catch (Exception e) {
			log.error("error calling student create API");
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", toUserMessage(e, admissionReq)));
		}
		

	}

	private String toUserMessage(Exception e, AdmissionRequestFromUI admissionReq) {
		String msg = e != null && e.getMessage() != null ? e.getMessage() : "Unable to save student data.";
		String lower = msg.toLowerCase();
		if (lower.contains("duplicate entry")) {
			String dupValue = extractDuplicateValue(msg);
			String reg = admissionReq != null ? admissionReq.getRegistrationNumber() : null;
			String aadhaar = admissionReq != null ? admissionReq.getAadhaar() : null;
			if (dupValue != null) {
				if (reg != null && reg.equals(dupValue)) {
					return "This registration number is already used.";
				}
				if (aadhaar != null && aadhaar.equals(dupValue)) {
					return "Invalid Aadhaar number.";
				}
			}
		}
		if (lower.contains("aadhaar") || lower.contains("aadhar")) {
			return "Invalid Aadhaar number.";
		}
		if (lower.contains("registration_number") || lower.contains("registration number")) {
			return "This registration number is already used.";
		}
		if (lower.contains("blood_group")) {
			return "Invalid blood group.";
		}
		if (lower.contains("data truncation") || lower.contains("data too long")) {
			return "Some fields are too long. Please shorten the input.";
		}
		return msg;
	}

	private String extractDuplicateValue(String message) {
		if (message == null) {
			return null;
		}
		Matcher matcher = Pattern.compile("Duplicate entry '([^']+)'").matcher(message);
		return matcher.find() ? matcher.group(1) : null;
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

	@PostMapping("/branch-approve")
	public ResponseEntity<Admission2> approveByBranch(
			@RequestParam Long id,
			@AuthenticationPrincipal OidcUser oidcUser
	) {
		String actor = oidcUser != null ? oidcUser.getFullName() : null;
		try {
			Admission2 admission = admissionApiClientService.approveByBranch(id, actor);
			return new ResponseEntity<>(admission, HttpStatus.OK);
		} catch (Exception e) {
			log.error("Failed to approve admission {} by branch", id, e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping(
			value = "/installments/partial-payment",
			consumes = MediaType.MULTIPART_FORM_DATA_VALUE
	)
	@ResponseBody
	public ResponseEntity<?> applyPartialPayment(
			@RequestParam Long admissionId,
			@RequestParam BigDecimal amount,
			@RequestParam String mode,
			@RequestParam(required = false) String txnRef,
			@RequestParam(required = false) String role,
			@RequestPart(required = false) MultipartFile receipt,
			@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
			@AuthenticationPrincipal OidcUser oidcUser
	) {
		String accessToken = client.getAccessToken().getTokenValue();
		String receivedBy = oidcUser != null ? oidcUser.getFullName() : null;

		UploadRequest receiptMeta = null;
		if (receipt != null && !receipt.isEmpty()) {
			try {
				var stored = fileStorageService.store(admissionId, receipt);
				String sha256;
				try (InputStream in = Files.newInputStream(stored.path())) {
					sha256 = HashUtil.sha256Hex(in);
				}
				receiptMeta = UploadRequest.builder()
						.docTypeCode("RECEIPT")
						.filename(receipt.getOriginalFilename())
						.mimeType(receipt.getContentType())
						.sizeBytes(Math.toIntExact(receipt.getSize()))
						.storageUrl(stored.url())
						.sha256(sha256)
						.label("PARTIAL_RECEIPT")
						.build();
			} catch (Exception e) {
				log.error("Failed to store partial payment receipt for admission {}", admissionId, e);
				return new ResponseEntity<>("Receipt upload failed", HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		PartialPaymentRequest request = new PartialPaymentRequest();
		request.setAmount(amount);
		request.setMode(mode);
		request.setTxnRef(txnRef);
		request.setReceivedBy(receivedBy);
		request.setReceipt(receiptMeta);

		boolean ok = admissionApiClientService.applyPartialPayment(admissionId, request, role, accessToken);
		if (!ok) {
			return new ResponseEntity<>("Failed to apply payment", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return ResponseEntity.ok("Payment saved");
	}

	@GetMapping("/installments/{installmentId}/payments")
	@ResponseBody
	public ResponseEntity<?> getInstallmentPayments(
			@PathVariable Long installmentId,
			@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client
	) {
		String accessToken = client.getAccessToken().getTokenValue();
		return ResponseEntity.ok(admissionApiClientService.getInstallmentPayments(installmentId, accessToken));
	}

	@PostMapping("/installments/payments/{paymentId}/verify")
	@ResponseBody
	public ResponseEntity<?> verifyInstallmentPayment(
			@PathVariable Long paymentId,
			@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
			@AuthenticationPrincipal OidcUser oidcUser
	) {
		String accessToken = client.getAccessToken().getTokenValue();
		String actor = oidcUser != null ? oidcUser.getFullName() : null;
		boolean ok = admissionApiClientService.verifyInstallmentPayment(paymentId, actor, accessToken);
		if (!ok) {
			return new ResponseEntity<>("Failed to verify payment", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return ResponseEntity.ok("Verified");
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
			@RequestParam(required = false) String studentAcknowledgementProofFileName,
			@RequestPart(required = false) MultipartFile studentAcknowledgementProof,
			@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
			@AuthenticationPrincipal OidcUser oidcUser
	) {
		String accessToken = client.getAccessToken().getTokenValue();

		// 🔹 FILE SAVE
		if (refundProof != null && !refundProof.isEmpty()) {


			// create directory if not exists
			Path admissionDir = Paths.get(cancelBaseDir, String.valueOf(admissionId));
			try {
				Files.createDirectories(admissionDir);

				Path targetFile = admissionDir.resolve(Objects.requireNonNull(refundProof.getOriginalFilename()));
				Files.copy(refundProof.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
				refundProofFileName=targetFile.getFileName().toString();
			} catch (IOException e) {
				throw new RuntimeException("File upload failed", e);
			}
		}


		// 🔹 Student Acknowledgement Proof FILE SAVE
		if (studentAcknowledgementProof != null && !studentAcknowledgementProof.isEmpty()) {
			try {
				Path admissionDir = Paths.get(cancelBaseDir, String.valueOf(admissionId));
				Files.createDirectories(admissionDir);

				Path targetFile = admissionDir.resolve(Objects.requireNonNull(studentAcknowledgementProof.getOriginalFilename()));
				Files.copy(studentAcknowledgementProof.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

				studentAcknowledgementProofFileName = targetFile.getFileName().toString();
			} catch (IOException e) {
				throw new RuntimeException("Student acknowledgement proof file upload failed", e);
			}
		}

		this.admissionApiClientService.cancelAdmissionDetailsUpdate(
				admissionId,
				cancelCharges,
				handlingPerson,
				remark,
				refundProofFileName,
				studentAcknowledgementProofFileName,
				accessToken
		);

		return ResponseEntity.ok("Cancellation details saved successfully");
	}

	@GetMapping("/download-cancellation-proof/{admissionId}/{fileName}")
	public ResponseEntity<Resource> downloadCancellationProof(
			@PathVariable Long admissionId,
			@PathVariable String fileName
	) throws IOException {

		Path filePath = Paths.get(cancelBaseDir,
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

	// Save Xerox / Original
	@PostMapping("/document-verification-save")
	public ResponseEntity<?> saveReceivedType(
			@RequestParam Long admissionId,
			@RequestParam String documentCode,
			@RequestParam String receivedType
	) {
		documentService.saveReceivedType(admissionId, documentCode, receivedType);
		return ResponseEntity.ok("Saved");
	}

	// HO Verify
	@PostMapping("/document-verification-verify")
	public ResponseEntity<?> verifyDocument(
			@RequestParam Long admissionId,
			@RequestParam String documentCode,
			@AuthenticationPrincipal OidcUser user
	) {
		documentService.verifyDocument(admissionId, documentCode, user.getFullName());
		return ResponseEntity.ok("Verified");
	}

	@PostMapping("/college-verification-verify")
	public ResponseEntity<?> verifyCollege(
			@RequestParam Long admissionId,
			@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
			@AuthenticationPrincipal OidcUser user
	) {
		String accessToken = client.getAccessToken().getTokenValue();
		admissionApiClientService.updateCollegeVerification(admissionId, "VERIFIED", user.getFullName(), accessToken);
		return ResponseEntity.ok("Verified");
	}

	@PostMapping("/college-verification-reject")
	public ResponseEntity<?> rejectCollege(
			@RequestParam Long admissionId,
			@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
			@AuthenticationPrincipal OidcUser user
	) {
		String accessToken = client.getAccessToken().getTokenValue();
		admissionApiClientService.updateCollegeVerification(admissionId, "REJECTED", user.getFullName(), accessToken);
		return ResponseEntity.ok("Rejected");
	}

}
