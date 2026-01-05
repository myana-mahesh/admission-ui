package com.impactsure.sanctionui.web;

import com.impactsure.sanctionui.dto.CancelAdmissionDTO;
import com.impactsure.sanctionui.dto.CollegeCourseSeatDto;
import com.impactsure.sanctionui.dto.FeeInvoiceDto;
import com.impactsure.sanctionui.dto.CourseDocumentRequirementDto;
import com.impactsure.sanctionui.dto.DocumentTypeOptionDto;
import com.impactsure.sanctionui.dto.MasterOptionDto;
import com.impactsure.sanctionui.dto.OtherPaymentFilterDto;
import com.impactsure.sanctionui.dto.StudentOtherPaymentValueDto;
import com.impactsure.sanctionui.dto.StudentPerkDto;
import com.impactsure.sanctionui.dto.StudentPerksMasterDto;

import com.impactsure.sanctionui.entities.*;
import com.impactsure.sanctionui.service.impl.*;
import lombok.RequiredArgsConstructor;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.servlet.ModelAndView;

import com.impactsure.sanctionui.dto.AdmissionDto;
import com.impactsure.sanctionui.dto.AdmissionListRowDto;
import com.impactsure.sanctionui.dto.PaymentModeDto;
import com.impactsure.sanctionui.dto.StudentDto;
import com.impactsure.sanctionui.enums.AdmissionStatus;
import com.impactsure.sanctionui.enums.Gender;
import com.impactsure.sanctionui.enums.GuardianRelation;
import com.impactsure.sanctionui.repository.AcademicYearRepository;
import com.impactsure.sanctionui.repository.CourseRepository;
import com.impactsure.sanctionui.repository.YearlyFeesRepository;
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
	private CollegeApiClientService collegeApiClientService;

	@Autowired
	private StudentApiClientService studentApiClientService;

	@Autowired
	private StudentPerksMasterService studentPerksMasterService;

	@Autowired
	private StudentPerkClientService studentPerkClientService;

	@Autowired
	private BatchMasterService batchMasterService;
	
	@Autowired
	private YearlyFeesRepository yearlyFeesRepository;
	
	@Autowired
	private PaymentModeApiClientService paymentModeApiClientService;
	
	@Autowired
	private InvoiceClient invoiceClient;

	@Autowired
	private AcademicYearRepository academicYearRepository;

	@Autowired
	private ReferenceDataClientService referenceDataClientService;

	@Autowired
	private OtherPaymentFieldApiClientService otherPaymentFieldApiClientService;

	@Autowired
	private CourseDocumentRequirementClientService courseDocumentRequirementClientService;

	@Autowired
	private ObjectMapper objectMapper;
	public List<String> getDiscountRemarkMasterList(String accessToken){
		List<String> reasons = new ArrayList<>();
		try {
			List<MasterOptionDto> masterReasons = referenceDataClientService.getDiscountReasons(accessToken);
			if (masterReasons != null) {
				for (MasterOptionDto reason : masterReasons) {
					if (reason != null && reason.getName() != null && !reason.getName().isBlank()) {
						reasons.add(reason.getName().trim());
					}
				}
			}
		} catch (Exception ex) {
			reasons.addAll(Arrays.asList("Other"));
		}
		if (reasons.stream().noneMatch(r -> "Other".equalsIgnoreCase(r))) {
			reasons.add("Other");
		}
		return reasons;
	}
	
	@Autowired
	private StudentDocumentVerificationService documentVerificationService;

	@Autowired
	private UserBranchMappingService userBranchMappingService;

	@Autowired
	private UserBatchMappingService userBatchMappingService;

	@Autowired
	private UserCourseMappingService userCourseMappingService;

	private final BranchService branchService;


	public List<String> clientRoleNames(OidcUser user){
		return user.getAuthorities().stream()
			      .map(a -> a.getAuthority())
			      .filter(a -> a.startsWith("ROLE_"))
			      .map(a -> a.substring("ROLE_".length()))
			      .toList();
	    	}
	
	public String getSingleRole(List<String> roles) {
		String role="";
		
		if(roles.contains("SUPER_ADMIN")) {
			role="SUPER_ADMIN";
		}else if(roles.contains("ADMIN")) {
			role="ADMIN";
		}else if(roles.contains("HO")) {
			role="HO";
		}else if(roles.contains("BRANCH_USER")) {
			role="BRANCH_USER";
		}
		return role;	 
	}

	private List<Long> resolveUserBranchIds(OidcUser oidcUser) {
		if (oidcUser == null || oidcUser.getSubject() == null) {
			return List.of();
		}
		return userBranchMappingService.getBranchIds(oidcUser.getSubject());
	}

	private List<Long> resolveUserBatchIds(OidcUser oidcUser) {
		if (oidcUser == null || oidcUser.getSubject() == null) {
			return List.of();
		}
		return userBatchMappingService.getBatchIds(oidcUser.getSubject());
	}

	private List<Long> resolveUserCourseIds(OidcUser oidcUser) {
		if (oidcUser == null || oidcUser.getSubject() == null) {
			return List.of();
		}
		return userCourseMappingService.getCourseIds(oidcUser.getSubject());
	}

	private List<String> resolveBatchCodes(List<Long> batchIds) {
		if (batchIds == null || batchIds.isEmpty()) {
			return List.of();
		}
		Map<Long, String> byId = batchMasterService.getAllBatches().stream()
				.filter(b -> b != null && b.getBatchId() != null)
				.collect(Collectors.toMap(b -> b.getBatchId(), b -> b.getCode(), (a, b) -> a));
		return batchIds.stream()
				.map(byId::get)
				.filter(c -> c != null && !c.isBlank())
				.toList();
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
		model.addObject("batches", batchMasterService.getAllBatches());
		model.addObject("colleges", collegeApiClientService.listAll(accessToken));
		
		List<PaymentModeDto> paymentModes = paymentModeApiClientService.getPaymentModes(accessToken);
		 List<String> paymentModeStrings = new ArrayList<>();
	        for(PaymentModeDto  mode:paymentModes) {
	        	paymentModeStrings.add(mode.getCode());
	        }
			List<String> roles = clientRoleNames(oidcUser);
	    String role = getSingleRole(roles);
	    model.addObject("role", role);
		model.addObject("branches", branchService.getAllBranches());
		model.addObject("nationalities", referenceDataClientService.getNationalities(accessToken));
		model.addObject("religions", referenceDataClientService.getReligions(accessToken));
		
		model.addObject("courses",courses);
		model.addObject("paymentModes", paymentModeStrings);
		model.addObject("otherPaymentFields", otherPaymentFieldApiClientService.listFields(false, accessToken));
		model.addObject("otherPaymentValues", new HashMap<Long, List<StudentOtherPaymentValueDto>>());
		model.addObject("perksList", studentPerksMasterService.getAllPerks(accessToken));
		
		
		List<String> discountRemarks =  getDiscountRemarkMasterList(accessToken);
		model.addObject("discountRemarks", discountRemarks);
		
		model.addObject("userName", oidcUser.getFullName());
		model.setViewName("admission-from");
		return model;
	}
	
	@GetMapping("/admissionlist")
	public ModelAndView listAdmissions(
	    @RequestParam(defaultValue = "") String q,
	    @RequestParam(defaultValue = "PENDING,ADMITTED,UNDER_CANCELLATION,CANCELLED") String status,
	    @RequestParam(defaultValue = "0") int page,
	    @RequestParam(defaultValue = "25") int size,
	    @RequestParam(required = false) String collegeId,
	    @RequestParam(required = false, name = "courseId") String courseIds,
	    @RequestParam(required = false) String batch,
	    @RequestParam(required = false) String admissionYearId,
	    @RequestParam(required = false) String gender,
	    @RequestParam(required = false) String perkId,
	    @RequestParam(required = false) String docTypeIds,
	    @RequestParam(required = false) String docReceived,
	    @RequestParam(required = false) String otherPaymentFilters,
	    @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
        @AuthenticationPrincipal OidcUser oidcUser
	    
	){
		ModelAndView model = new ModelAndView();
		String accessToken = client.getAccessToken().getTokenValue();
		
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

	    String safeCollegeId = normalizeBlank(collegeId);
	    String safeCourseIds = normalizeBlank(courseIds);
	    String safeBatch = normalizeBlank(batch);
	    String safeAdmissionYearId = normalizeBlank(admissionYearId);
	    String safeGender = normalizeBlank(gender);
	    String safePerkId = normalizeBlank(perkId);

	    Long collegeIdValue = parseLongOrNull(safeCollegeId);
	    List<Long> courseIdValues = parseLongListOrEmpty(safeCourseIds);
	    Long yearIdValue = parseLongOrNull(safeAdmissionYearId);
	    Gender genderValue = parseGenderOrNull(safeGender);
	    List<Long> docTypeIdValues = parseLongListOrEmpty(normalizeBlank(docTypeIds));
	    Boolean docReceivedValue = parseDocumentReceived(docReceived);

	    List<Long> perkStudentIds = fetchStudentIdsByPerk(safePerkId, accessToken);
	    List<OtherPaymentFilterDto> otherPaymentFilterList = parseOtherPaymentFilters(otherPaymentFilters);

	    List<String> roles = clientRoleNames(oidcUser);
	    String role = getSingleRole(roles);
	    boolean isSuperAdmin = roles.contains("SUPER_ADMIN");
	    boolean isHo = roles.contains("HO");
	    List<Long> userBranchIds = (isSuperAdmin || isHo) ? null : resolveUserBranchIds(oidcUser);
	    List<Long> userCourseIds = (isSuperAdmin || isHo) ? null : resolveUserCourseIds(oidcUser);
	    List<Long> userBatchIds = (isSuperAdmin || isHo) ? null : resolveUserBatchIds(oidcUser);
	    List<String> userBatchCodes = (isSuperAdmin || isHo) ? null : resolveBatchCodes(userBatchIds);
	    // Add roles to your model (or use them for access control)
	    model.addObject("role", role);

	    Boolean branchApprovedOnly = "HO".equals(role) ? Boolean.TRUE : null;
	    Page<Admission2> result = null;
	    List<Long> effectiveCourseIds = courseIdValues;
	    String effectiveBatch = safeBatch;
	    List<String> effectiveBatchCodes = null;
	    boolean allowNullCourse = false;
	    boolean allowNullBatch = false;
	    if (!isSuperAdmin && !isHo) {
	    	if (userBranchIds == null || userBranchIds.isEmpty()
	    			|| userCourseIds == null || userCourseIds.isEmpty()
	    			|| userBatchCodes == null || userBatchCodes.isEmpty()) {
	    		result = Page.empty(pageable);
	    	} else {
	    		if (!effectiveCourseIds.isEmpty()) {
	    			effectiveCourseIds = effectiveCourseIds.stream()
	    					.filter(userCourseIds::contains)
	    					.toList();
	    			if (effectiveCourseIds.isEmpty()) {
	    				result = Page.empty(pageable);
	    			}
	    		} else {
	    			effectiveCourseIds = userCourseIds;
	    			allowNullCourse = true;
	    		}
	    		if (result == null) {
	    			if (effectiveBatch != null && !effectiveBatch.isBlank()) {
	    				if (!userBatchCodes.contains(effectiveBatch)) {
	    					result = Page.empty(pageable);
	    				}
	    			} else {
	    				effectiveBatch = null;
	    				effectiveBatchCodes = userBatchCodes;
	    			allowNullBatch = true;
	    			}
	    		}
	    	}
	    }
	    if (result == null) {
	    	result = admissionApiClientService.searchAdmissionsFiltered(
	    			q, status, page, size, collegeIdValue, effectiveCourseIds,
	    			effectiveBatch, effectiveBatchCodes, allowNullCourse, allowNullBatch, yearIdValue, genderValue,
	    			perkStudentIds, userBranchIds, branchApprovedOnly,
	    			docTypeIdValues, docReceivedValue, otherPaymentFilterList
	    	);
	    }
		    
	    model.addObject("page", result);
	    model.addObject("q", q);
	    model.addObject("size", size);
	    model.addObject("statusSelected", status);
	    model.addObject("collegeId", safeCollegeId);
	    model.addObject("courseId", safeCourseIds);
	    model.addObject("batch", safeBatch);
	    model.addObject("admissionYearId", safeAdmissionYearId);
	    model.addObject("gender", safeGender);
	    model.addObject("perkId", safePerkId);
	    model.addObject("docTypeIds", normalizeBlank(docTypeIds));
	    model.addObject("docReceived", docReceived);
	    List<BatchMaster> batches = batchMasterService.getAllBatches();
	    List<Course> courses = courseRepository.findAll();
	    if (!isSuperAdmin && !isHo) {
	    	if (userBatchIds == null || userBatchIds.isEmpty()) {
	    		batches = List.of();
	    	} else {
	    		batches = batches.stream()
	    				.filter(b -> b != null && b.getBatchId() != null && userBatchIds.contains(b.getBatchId()))
	    				.toList();
	    	}
	    	if (userCourseIds == null || userCourseIds.isEmpty()) {
	    		courses = List.of();
	    	} else {
	    		courses = courses.stream()
	    				.filter(c -> c != null && c.getCourseId() != null && userCourseIds.contains(c.getCourseId()))
	    				.toList();
	    	}
	    }
	    model.addObject("batches", batches);
	    model.addObject("courses", courses);
	    model.addObject("academicYears", academicYearRepository.findAll());
	    model.addObject("colleges", collegeApiClientService.listAll(accessToken));
	    model.addObject("perks", studentPerksMasterService.getAllPerks(accessToken));
	    List<CourseDocumentRequirementDto> requirements = courseDocumentRequirementClientService.listRequirements(accessToken);
	    List<DocumentTypeOptionDto> docTypeOptions = courseDocumentRequirementClientService.listDocumentTypes(accessToken);
	    model.addObject("courseDocRequirements", requirements);
	    model.addObject("docTypeOptions", docTypeOptions);
	    model.addObject("otherPaymentFields", otherPaymentFieldApiClientService.listFields(false, accessToken));
	    model.setViewName("admissions/admissions-list");
	    return model;
	}

	@GetMapping("/api/admissionlist")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> listAdmissionsJson(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "25") int size,
			@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "PENDING,ADMITTED,UNDER_CANCELLATION,CANCELLED") String status,
			@RequestParam(required = false) String collegeId,
			@RequestParam(required = false, name = "courseId") String courseIds,
			@RequestParam(required = false) String batch,
			@RequestParam(required = false) String admissionYearId,
			@RequestParam(required = false) String gender,
			@RequestParam(required = false) String perkId,
			@RequestParam(required = false) String docTypeIds,
			@RequestParam(required = false) String docReceived,
			@RequestParam(required = false) String otherPaymentFilters,
			@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
	        @AuthenticationPrincipal OidcUser oidcUser
	) {
		String accessToken = client.getAccessToken().getTokenValue();

		String safeCollegeId = normalizeBlank(collegeId);
		String safeCourseIds = normalizeBlank(courseIds);
		String safeBatch = normalizeBlank(batch);
		String safeAdmissionYearId = normalizeBlank(admissionYearId);
		String safeGender = normalizeBlank(gender);
		String safePerkId = normalizeBlank(perkId);

		Long collegeIdValue = parseLongOrNull(safeCollegeId);
		List<Long> courseIdValues = parseLongListOrEmpty(safeCourseIds);
		Long yearIdValue = parseLongOrNull(safeAdmissionYearId);
		Gender genderValue = parseGenderOrNull(safeGender);
		List<Long> docTypeIdValues = parseLongListOrEmpty(normalizeBlank(docTypeIds));
		Boolean docReceivedValue = parseDocumentReceived(docReceived);

		List<Long> perkStudentIds = fetchStudentIdsByPerk(safePerkId, accessToken);
		List<OtherPaymentFilterDto> otherPaymentFilterList = parseOtherPaymentFilters(otherPaymentFilters);

		List<String> roles = clientRoleNames(oidcUser);
	    String role = getSingleRole(roles);
	    boolean isSuperAdmin = roles.contains("SUPER_ADMIN");
	    boolean isHo = roles.contains("HO");
	    List<Long> userBranchIds = (isSuperAdmin || isHo) ? null : resolveUserBranchIds(oidcUser);
	    List<Long> userCourseIds = (isSuperAdmin || isHo) ? null : resolveUserCourseIds(oidcUser);
	    List<Long> userBatchIds = (isSuperAdmin || isHo) ? null : resolveUserBatchIds(oidcUser);
	    List<String> userBatchCodes = (isSuperAdmin || isHo) ? null : resolveBatchCodes(userBatchIds);
	    Boolean branchApprovedOnly = "HO".equals(role) ? Boolean.TRUE : null;

		Page<Admission2> result = null;
		List<Long> effectiveCourseIds = courseIdValues;
		String effectiveBatch = safeBatch;
		List<String> effectiveBatchCodes = null;
		boolean allowNullCourse = false;
		boolean allowNullBatch = false;
		if (!isSuperAdmin && !isHo) {
			if (userBranchIds == null || userBranchIds.isEmpty()
					|| userCourseIds == null || userCourseIds.isEmpty()
					|| userBatchCodes == null || userBatchCodes.isEmpty()) {
				Pageable pageable = PageRequest.of(Math.max(page,0), Math.min(size,100), Sort.by(Sort.Direction.DESC,"createdAt"));
				result = Page.empty(pageable);
			} else {
				if (!effectiveCourseIds.isEmpty()) {
					effectiveCourseIds = effectiveCourseIds.stream()
							.filter(userCourseIds::contains)
							.toList();
					if (effectiveCourseIds.isEmpty()) {
						Pageable pageable = PageRequest.of(Math.max(page,0), Math.min(size,100), Sort.by(Sort.Direction.DESC,"createdAt"));
						result = Page.empty(pageable);
					}
				} else {
					effectiveCourseIds = userCourseIds;
					allowNullCourse = true;
				}
				if (result == null) {
					if (effectiveBatch != null && !effectiveBatch.isBlank()) {
						if (!userBatchCodes.contains(effectiveBatch)) {
							Pageable pageable = PageRequest.of(Math.max(page,0), Math.min(size,100), Sort.by(Sort.Direction.DESC,"createdAt"));
							result = Page.empty(pageable);
						}
					} else {
						effectiveBatch = null;
						effectiveBatchCodes = userBatchCodes;
						allowNullBatch = true;
					}
				}
			}
		}
		if (result == null) {
			result = admissionApiClientService.searchAdmissionsFiltered(
					q, status, page, size, collegeIdValue, effectiveCourseIds,
					effectiveBatch, effectiveBatchCodes, allowNullCourse, allowNullBatch, yearIdValue, genderValue,
					perkStudentIds, userBranchIds, branchApprovedOnly,
					docTypeIdValues, docReceivedValue, otherPaymentFilterList
			);
		}

		List<AdmissionListRowDto> rows = result.getContent().stream().map(admission -> {
			AdmissionListRowDto row = new AdmissionListRowDto();
			row.setAdmissionId(admission.getAdmissionId());
			row.setStatus(admission.getStatus() != null ? admission.getStatus().name() : null);
			row.setCreatedAt(admission.getCreatedAt());
			if (admission.getStudent() != null) {
				row.setAbsId(admission.getStudent().getAbsId());
				row.setStudentName(admission.getStudent().getFullName());
				row.setStudentMobile(admission.getStudent().getMobile());
			}
			if (admission.getCourse() != null) {
				row.setCourseName(admission.getCourse().getName());
			}
			row.setBranchApproved(admission.getBranchApproved());
			return row;
		}).toList();

		Map<String, Object> pageMap = new HashMap<>();
		pageMap.put("content", rows);
		pageMap.put("number", result.getNumber());
		pageMap.put("size", result.getSize());
		pageMap.put("totalPages", result.getTotalPages());
		pageMap.put("totalElements", result.getTotalElements());
		pageMap.put("numberOfElements", result.getNumberOfElements());
		pageMap.put("first", result.isFirst());
		pageMap.put("last", result.isLast());

		Map<String, Object> response = new HashMap<>();
		response.put("page", pageMap);
		return ResponseEntity.ok(response);
	}

	private List<OtherPaymentFilterDto> parseOtherPaymentFilters(String payload) {
		if (payload == null || payload.isBlank()) {
			return List.of();
		}
		try {
			return objectMapper.readValue(payload, new TypeReference<List<OtherPaymentFilterDto>>() {});
		} catch (Exception ex) {
			return List.of();
		}
	}

	private String normalizeBlank(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private Long parseLongOrNull(String value) {
		if (value == null) {
			return null;
		}
		try {
			return Long.valueOf(value);
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private List<Long> parseLongListOrEmpty(String csv) {
		if (csv == null || csv.isBlank()) {
			return List.of();
		}
		String[] parts = csv.split(",");
		List<Long> values = new ArrayList<>();
		for (String part : parts) {
			String trimmed = part.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			try {
				values.add(Long.valueOf(trimmed));
			} catch (NumberFormatException ignored) {
				// skip invalid ids
			}
		}
		return values;
	}

	private Boolean parseDocumentReceived(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		if ("RECEIVED".equalsIgnoreCase(value)) {
			return Boolean.TRUE;
		}
		if ("NOT_RECEIVED".equalsIgnoreCase(value) || "PENDING".equalsIgnoreCase(value)) {
			return Boolean.FALSE;
		}
		return null;
	}

	private Gender parseGenderOrNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String normalized = value.trim();
		for (Gender gender : Gender.values()) {
			if (gender.name().equalsIgnoreCase(normalized)) {
				return gender;
			}
		}
		return null;
	}

	private List<Long> fetchStudentIdsByPerk(String perkId, String accessToken) {
		if (perkId == null) {
			return null;
		}
		List<Long> studentIds = new ArrayList<>();
		int page = 0;
		int size = 1000;
		try {
			while (true) {
				var pageData = studentApiClientService.getStudentsByFilter(
						page, size, null, null, null, null, null, perkId, null, accessToken
				);
				if (pageData == null || pageData.getContent() == null) {
					break;
				}
				pageData.getContent().stream()
						.map(StudentDto::getStudentId)
						.filter(id -> id != null)
						.forEach(studentIds::add);
				if (pageData.isLast() || page >= pageData.getTotalPages() - 1) {
					break;
				}
				page++;
			}
		} catch (Exception ex) {
			return List.of();
		}
		return studentIds;
	}

	@GetMapping("/college-courses")
	@ResponseBody
	public List<CollegeCourseSeatDto> listCollegeCourses(
			@RequestParam Long collegeId,
			@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
			@AuthenticationPrincipal OidcUser oidcUser
	) {
		String accessToken = client.getAccessToken().getTokenValue();
		return collegeApiClientService.getCollegeCourseSeats(collegeId, accessToken);
	}
	
	@GetMapping("/admissions")
	public ModelAndView viewAdmission(@RequestParam Long id,
			@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
	        @AuthenticationPrincipal OidcUser oidcUser) {
		ModelAndView model = new ModelAndView();
		String accessToken = client.getAccessToken().getTokenValue();

	    Admission2 admission = admissionApiClientService.getAdmissionById(id);
	    List<String> roles = clientRoleNames(oidcUser);
	    boolean isSuperAdmin = roles.contains("SUPER_ADMIN");
	    boolean isHo = roles.contains("HO");
	    if (!isSuperAdmin && !isHo) {
	    	List<Long> userBranchIds = resolveUserBranchIds(oidcUser);
	    	if (!isAdmissionInBranches(admission, userBranchIds)) {
	    		return new ModelAndView("redirect:/admissionlist");
	    	}
	    	List<Long> userCourseIds = resolveUserCourseIds(oidcUser);
	    	if (userCourseIds == null || userCourseIds.isEmpty()) {
	    		return new ModelAndView("redirect:/admissionlist");
	    	}
	    	if (admission.getCourse() != null && admission.getCourse().getCourseId() != null
	    			&& !userCourseIds.contains(admission.getCourse().getCourseId())) {
	    		return new ModelAndView("redirect:/admissionlist");
	    	}
	    	List<Long> userBatchIds = resolveUserBatchIds(oidcUser);
	    	List<String> userBatchCodes = resolveBatchCodes(userBatchIds);
	    	if (userBatchCodes == null || userBatchCodes.isEmpty()) {
	    		return new ModelAndView("redirect:/admissionlist");
	    	}
	    	String admissionBatch = admission.getBatch();
	    	if (admissionBatch != null && !userBatchCodes.contains(admissionBatch)) {
	    		return new ModelAndView("redirect:/admissionlist");
	    	}
	    }
	    
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

		// NEW
		Map<String, StudentDocumentVerification> verificationMap = documentVerificationService.getVerificationMap(id);

		model.addObject("branches", branchService.getAllBranches());
	    String role = getSingleRole(roles);
	    model.addObject("role", role);
        model.addObject("id", id);
        model.addObject("father", father);
        model.addObject("mother", mother);
	    model.addObject("admission", admission);
	    model.addObject("courses", courseRepository.findAll());
	    model.addObject("colleges", collegeApiClientService.listAll(accessToken));
	    model.addObject("batches", batchMasterService.getAllBatches());
	    model.addObject("nationalities", referenceDataClientService.getNationalities(accessToken));
	    model.addObject("religions", referenceDataClientService.getReligions(accessToken));
	    model.addObject("receipts", receipts);
	    model.addObject("docUploads", docUploads);
	    model.addObject("yearlyFees", yearlyFeesMap); 
	    model.addObject("paymentModes", paymentModeStrings);
		model.addObject("cancellation", cancelAdmissionDTO);
		model.addObject("verificationMap", verificationMap);
		model.addObject("otherPaymentFields", otherPaymentFieldApiClientService.listFields(false, accessToken));
		List<StudentOtherPaymentValueDto> otherPaymentValues = otherPaymentFieldApiClientService
				.listStudentValues(admission.getStudent().getStudentId(), accessToken);
		Map<Long, List<StudentOtherPaymentValueDto>> otherPaymentMap = otherPaymentValues.stream()
				.collect(Collectors.groupingBy(StudentOtherPaymentValueDto::getFieldId));
		model.addObject("otherPaymentValues", otherPaymentMap);
		List<StudentPerksMasterDto> perks = this.studentPerksMasterService.getAllPerks(accessToken);
		model.addObject("perksList", perks);

		List<StudentPerkDto> studentPerks = this.studentPerkClientService
				.getPerksForStudent(admission.getStudent().getStudentId(), accessToken);
		List<Long> studentPerkIds = extractPerkIds(studentPerks);
		model.addObject("studentPerkIds", studentPerkIds);
	    model.setViewName("admissions/admission-view");
	    
	    model.addObject("hasExistingInstallments", !admission.getInstallments().isEmpty());
	    model.addObject("existingInstallments", admission.getInstallments());
	    
	    List<FeeInvoiceDto> invoices =
                invoiceClient.getInvoicesForAdmission(admission.getAdmissionId(), accessToken);

        // 3) Convert to Map<installmentId, FeeInvoiceDto> for easy lookup in Thymeleaf
        Map<Long, FeeInvoiceDto> invoiceMap = invoices.stream()
                .filter(inv -> inv.getInstallmentId() != null)
                .collect(Collectors.toMap(
                        FeeInvoiceDto::getInstallmentId,
                        Function.identity(),
                        (a, b) -> a      // in case of duplicates, keep first
                ));

        model.addObject("invoices", invoiceMap);
        
        List<String> discountRemarks =  getDiscountRemarkMasterList(accessToken);
		model.addObject("discountRemarks", discountRemarks);

		model.addObject("userName", oidcUser.getFullName());
		
	    return model; 
	}

	private boolean isAdmissionInBranches(Admission2 admission, List<Long> branchIds) {
		if (admission == null || branchIds == null || branchIds.isEmpty()) {
			return false;
		}
		if (admission.getAdmissionBranch() == null || admission.getAdmissionBranch().getId() == null) {
			return false;
		}
		return branchIds.contains(admission.getAdmissionBranch().getId());
	}

	private List<Long> extractPerkIds(List<StudentPerkDto> perks) {
		if (perks == null) {
			return List.of();
		}
		return perks.stream()
				.map(StudentPerkDto::getPerkId)
				.collect(Collectors.toList());
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
