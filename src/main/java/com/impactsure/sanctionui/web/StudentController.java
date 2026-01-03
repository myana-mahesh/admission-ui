package com.impactsure.sanctionui.web;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
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
import com.impactsure.sanctionui.dto.FeeInvoiceDto;
import com.impactsure.sanctionui.entities.Admission2;
import com.impactsure.sanctionui.entities.Student;
import com.impactsure.sanctionui.repository.AcademicYearRepository;
import com.impactsure.sanctionui.repository.Admission2Repository;
import com.impactsure.sanctionui.repository.CourseRepository;
import com.impactsure.sanctionui.service.impl.CollegeApiClientService;
import com.impactsure.sanctionui.service.impl.BatchMasterService;
import com.impactsure.sanctionui.service.impl.AdmissionApiClientService;
import com.impactsure.sanctionui.service.impl.InvoiceClient;
import com.impactsure.sanctionui.service.impl.ReferenceDataClientService;
import com.impactsure.sanctionui.service.impl.StudentApiClientService;
import com.impactsure.sanctionui.service.impl.StudentDocumentVerificationService;
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

	@Autowired
	private BatchMasterService batchMasterService;

	@Autowired
	private CourseRepository courseRepository;

	@Autowired
	private AcademicYearRepository academicYearRepository;

	@Autowired
	private CollegeApiClientService collegeApiClientService;

	@Autowired
	private Admission2Repository admission2Repository;

	@Autowired
	private AdmissionApiClientService admissionApiClientService;
	@Autowired
	private StudentDocumentVerificationService documentVerificationService;

	@Autowired
	private InvoiceClient invoiceClient;

	@Autowired
	private ReferenceDataClientService referenceDataClientService;

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
        model.addAttribute("batches", batchMasterService.getAllBatches());
        model.addAttribute("courses", courseRepository.findAll());
        model.addAttribute("academicYears", academicYearRepository.findAll());
        model.addAttribute("colleges", collegeApiClientService.listAll(accessToken));
        model.addAttribute("perks", studentPerksMasterService.getAllPerks(accessToken));
        addAdmissionSummaries(studentsPage, model);

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
        model.addAttribute("nationalities", referenceDataClientService.getNationalities(accessToken));
        model.addAttribute("religions", referenceDataClientService.getReligions(accessToken));
        admission2Repository.findTopByStudentStudentIdOrderByAdmissionIdDesc(student.getStudentId())
                .ifPresent(admission -> {
                    model.addAttribute("admission", admission);
                    model.addAttribute("docUploads",
                            admissionApiClientService.findDocUploadMapForAdmission(admission.getAdmissionId()));
                    model.addAttribute("verificationMap",
                            documentVerificationService.getVerificationMap(admission.getAdmissionId()));

                    BigDecimal totalFees = admission.getTotalFees() != null
                            ? BigDecimal.valueOf(admission.getTotalFees())
                            : BigDecimal.ZERO;
                    BigDecimal paidFees = admission.getInstallments() == null ? BigDecimal.ZERO
                            : admission.getInstallments().stream()
                                    .filter(inst -> inst.getStatus() != null
                                            && inst.getStatus().equalsIgnoreCase("Paid"))
                                    .map(inst -> inst.getAmountDue() == null ? BigDecimal.ZERO : inst.getAmountDue())
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal pendingFees = totalFees.subtract(paidFees);
                    if (pendingFees.compareTo(BigDecimal.ZERO) < 0) {
                        pendingFees = BigDecimal.ZERO;
                    }
                    model.addAttribute("totalFees", totalFees);
                    model.addAttribute("paidFees", paidFees);
                    model.addAttribute("pendingFees", pendingFees);

                    List<FeeInvoiceDto> invoices =
                            invoiceClient.getInvoicesForAdmission(admission.getAdmissionId(), accessToken);
                    Map<Long, FeeInvoiceDto> invoiceMap = invoices.stream()
                            .filter(inv -> inv.getInstallmentId() != null)
                            .collect(Collectors.toMap(
                                    FeeInvoiceDto::getInstallmentId,
                                    Function.identity(),
                                    (a, b) -> a
                            ));
                    model.addAttribute("invoices", invoiceMap);
                });
        
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
				    .sscDetails(admissionReq.getSscDetails())
				    .hscDetails(admissionReq.getHscDetails())
				    .batch(admissionReq.getBatch())
				    .registrationNumber(admissionReq.getRegistrationNumber())
				    .referenceName(admissionReq.getReferenceName())
				    .age(admissionReq.getAge())
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


	@GetMapping("/studentlist-filters")
	public String listStudents(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) String q,

			@RequestParam(required = false) String courseId,
			@RequestParam(required = false) String collegeId,
			@RequestParam(required = false) String batch,
			@RequestParam(required = false) String admissionYearId,
			@RequestParam(required = false) String perkId,
			@RequestParam(required = false) String gender,

			@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
			@AuthenticationPrincipal OidcUser oidcUser,
			Model model
	) {

		String accessToken = client.getAccessToken().getTokenValue();

		String safeCourseId = normalizeBlank(courseId);
		String safeCollegeId = normalizeBlank(collegeId);
		String safeBatch = normalizeBlank(batch);
		String safeAdmissionYearId = normalizeBlank(admissionYearId);
		String safePerkId = normalizeBlank(perkId);
		String safeGender = normalizeBlank(gender);

		PagedResponse<StudentDto> studentsPage =
				studentApiClientService.getStudentsByFilter(
						page, size, q, safeCourseId, safeCollegeId, safeBatch, safeAdmissionYearId, safePerkId, safeGender, accessToken
				);

		model.addAttribute("page", studentsPage);
		model.addAttribute("q", q);
		model.addAttribute("size", size);

		// Keep filters for UI persistence
		model.addAttribute("courseId", safeCourseId);
		model.addAttribute("collegeId", safeCollegeId);
		model.addAttribute("batch", safeBatch);
		model.addAttribute("admissionYearId", safeAdmissionYearId);
		model.addAttribute("perkId", safePerkId);
		model.addAttribute("gender", safeGender);
		model.addAttribute("batches", batchMasterService.getAllBatches());
		model.addAttribute("courses", courseRepository.findAll());
		model.addAttribute("academicYears", academicYearRepository.findAll());
		model.addAttribute("colleges", collegeApiClientService.listAll(accessToken));
		model.addAttribute("perks", studentPerksMasterService.getAllPerks(accessToken));
		addAdmissionSummaries(studentsPage, model);

		return "studentlist";
	}

	@GetMapping("/api/studentlist")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> getStudentListJson(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String courseId,
			@RequestParam(required = false) String collegeId,
			@RequestParam(required = false) String batch,
			@RequestParam(required = false) String admissionYearId,
			@RequestParam(required = false) String perkId,
			@RequestParam(required = false) String gender,
			@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
			@AuthenticationPrincipal OidcUser oidcUser
	) {
		String accessToken = client.getAccessToken().getTokenValue();

		String safeCourseId = normalizeBlank(courseId);
		String safeCollegeId = normalizeBlank(collegeId);
		String safeBatch = normalizeBlank(batch);
		String safeAdmissionYearId = normalizeBlank(admissionYearId);
		String safePerkId = normalizeBlank(perkId);
		String safeGender = normalizeBlank(gender);

		PagedResponse<StudentDto> studentsPage =
				studentApiClientService.getStudentsByFilter(
						page, size, q, safeCourseId, safeCollegeId, safeBatch, safeAdmissionYearId, safePerkId, safeGender, accessToken
				);

		// Build admission summaries using same logic as addAdmissionSummaries
		Map<Long, Admission2> admissionByStudentId = new HashMap<>();
		Map<Long, BigDecimal> paidFeesByStudentId = new HashMap<>();
		Map<Long, BigDecimal> pendingFeesByStudentId = new HashMap<>();

		if (studentsPage != null && studentsPage.getContent() != null && !studentsPage.getContent().isEmpty()) {
			List<Long> studentIds = studentsPage.getContent().stream()
					.map(StudentDto::getStudentId)
					.filter(Objects::nonNull)
					.toList();

			if (!studentIds.isEmpty()) {
				List<Admission2> admissions = admission2Repository.findLatestByStudentIds(studentIds);

				for (Admission2 admission : admissions) {
					if (admission.getStudent() == null || admission.getStudent().getStudentId() == null) {
						continue;
					}
					Long studentId = admission.getStudent().getStudentId();
					admissionByStudentId.put(studentId, admission);

					BigDecimal totalFees = admission.getTotalFees() != null
							? BigDecimal.valueOf(admission.getTotalFees())
							: BigDecimal.ZERO;
					BigDecimal paidFees = admission.getInstallments() == null ? BigDecimal.ZERO
							: admission.getInstallments().stream()
									.filter(inst -> inst.getStatus() != null
											&& inst.getStatus().equalsIgnoreCase("Paid"))
									.map(inst -> inst.getAmountDue() == null ? BigDecimal.ZERO : inst.getAmountDue())
									.reduce(BigDecimal.ZERO, BigDecimal::add);
					BigDecimal pendingFees = totalFees.subtract(paidFees);
					if (pendingFees.compareTo(BigDecimal.ZERO) < 0) {
						pendingFees = BigDecimal.ZERO;
					}

					paidFeesByStudentId.put(studentId, paidFees);
					pendingFeesByStudentId.put(studentId, pendingFees);
				}
			}
		}

		Map<String, Object> response = new HashMap<>();
		response.put("page", studentsPage);
		response.put("admissionByStudentId", admissionByStudentId);
		response.put("paidFeesByStudentId", paidFeesByStudentId);
		response.put("pendingFeesByStudentId", pendingFeesByStudentId);

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	private String normalizeBlank(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private void addAdmissionSummaries(PagedResponse<StudentDto> studentsPage, Model model) {
		if (studentsPage == null || studentsPage.getContent() == null || studentsPage.getContent().isEmpty()) {
			model.addAttribute("admissionByStudentId", Collections.emptyMap());
			model.addAttribute("paidFeesByStudentId", Collections.emptyMap());
			model.addAttribute("pendingFeesByStudentId", Collections.emptyMap());
			return;
		}

		List<Long> studentIds = studentsPage.getContent().stream()
				.map(StudentDto::getStudentId)
				.filter(Objects::nonNull)
				.toList();
		if (studentIds.isEmpty()) {
			model.addAttribute("admissionByStudentId", Collections.emptyMap());
			model.addAttribute("paidFeesByStudentId", Collections.emptyMap());
			model.addAttribute("pendingFeesByStudentId", Collections.emptyMap());
			return;
		}

		List<Admission2> admissions = admission2Repository.findLatestByStudentIds(studentIds);
		Map<Long, Admission2> admissionByStudentId = new HashMap<>();
		Map<Long, BigDecimal> paidFeesByStudentId = new HashMap<>();
		Map<Long, BigDecimal> pendingFeesByStudentId = new HashMap<>();

		for (Admission2 admission : admissions) {
			if (admission.getStudent() == null || admission.getStudent().getStudentId() == null) {
				continue;
			}
			Long studentId = admission.getStudent().getStudentId();
			admissionByStudentId.put(studentId, admission);

			BigDecimal totalFees = admission.getTotalFees() != null
					? BigDecimal.valueOf(admission.getTotalFees())
					: BigDecimal.ZERO;
			BigDecimal paidFees = admission.getInstallments() == null ? BigDecimal.ZERO
					: admission.getInstallments().stream()
							.filter(inst -> inst.getStatus() != null
									&& inst.getStatus().equalsIgnoreCase("Paid"))
							.map(inst -> inst.getAmountDue() == null ? BigDecimal.ZERO : inst.getAmountDue())
							.reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal pendingFees = totalFees.subtract(paidFees);
			if (pendingFees.compareTo(BigDecimal.ZERO) < 0) {
				pendingFees = BigDecimal.ZERO;
			}

			paidFeesByStudentId.put(studentId, paidFees);
			pendingFeesByStudentId.put(studentId, pendingFees);
		}

		model.addAttribute("admissionByStudentId", admissionByStudentId);
		model.addAttribute("paidFeesByStudentId", paidFeesByStudentId);
		model.addAttribute("pendingFeesByStudentId", pendingFeesByStudentId);
	}

}
