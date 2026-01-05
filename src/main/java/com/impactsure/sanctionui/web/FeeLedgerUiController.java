package com.impactsure.sanctionui.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.impactsure.sanctionui.dto.FeeLedgerResponseDto;
import com.impactsure.sanctionui.dto.FeeInstallmentPaymentDto;
import com.impactsure.sanctionui.dto.PaymentModeDto;
import com.impactsure.sanctionui.entities.Admission2;
import com.impactsure.sanctionui.entities.BranchMaster;
import com.impactsure.sanctionui.entities.FeeInstallment;
import com.impactsure.sanctionui.entities.FileUpload;
import com.impactsure.sanctionui.repository.AcademicYearRepository;
import com.impactsure.sanctionui.repository.CourseRepository;
import com.impactsure.sanctionui.service.impl.AdmissionApiClientService;
import com.impactsure.sanctionui.service.impl.BatchMasterService;
import com.impactsure.sanctionui.service.impl.BranchService;
import com.impactsure.sanctionui.service.impl.FeeLedgerClientService;
import com.impactsure.sanctionui.service.impl.PaymentModeApiClientService;
import com.impactsure.sanctionui.service.impl.StudentFeeCommentClientService;
import com.impactsure.sanctionui.service.impl.StudentFeeScheduleClientService;
import com.impactsure.sanctionui.service.impl.UserBatchMappingService;
import com.impactsure.sanctionui.service.impl.UserBranchMappingService;
import com.impactsure.sanctionui.service.impl.UserCourseMappingService;
import com.impactsure.sanctionui.dto.CreateStudentFeeCommentRequest;
import com.impactsure.sanctionui.dto.StudentFeeCommentDto;
import com.impactsure.sanctionui.dto.CreateStudentFeeScheduleRequest;
import com.impactsure.sanctionui.dto.StudentFeeScheduleDto;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class FeeLedgerUiController {

    private final FeeLedgerClientService feeLedgerClientService;
    private final PaymentModeApiClientService paymentModeApiClientService;
    private final CourseRepository courseRepository;
    private final AcademicYearRepository academicYearRepository;
    private final BatchMasterService batchMasterService;
    private final BranchService branchService;
    private final AdmissionApiClientService admissionApiClientService;
    private final StudentFeeCommentClientService studentFeeCommentClientService;
    private final StudentFeeScheduleClientService studentFeeScheduleClientService;
    private final UserBranchMappingService userBranchMappingService;
    private final UserBatchMappingService userBatchMappingService;
    private final UserCourseMappingService userCourseMappingService;

    @GetMapping("/fees-ledger")
    public String feesLedger(
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
            @AuthenticationPrincipal OidcUser oidcUser,
            Model model
    ) {
        String accessToken = client.getAccessToken().getTokenValue();
        boolean isSuperAdmin = hasRole(oidcUser, "SUPER_ADMIN");
        boolean isHo = hasRole(oidcUser, "HO");
        List<Long> userBranchIds = (isSuperAdmin || isHo) ? List.of() : resolveUserBranchIds(oidcUser);
        List<Long> userCourseIds = (isSuperAdmin || isHo) ? List.of() : resolveUserCourseIds(oidcUser);
        List<Long> userBatchIds = (isSuperAdmin || isHo) ? List.of() : resolveUserBatchIds(oidcUser);

        List<PaymentModeDto> paymentModes = paymentModeApiClientService.getPaymentModes(accessToken);
        List<BranchMaster> branches = branchService.getAllBranches();
        if (!isSuperAdmin && !isHo) {
            if (userBranchIds == null || userBranchIds.isEmpty()) {
                branches = List.of();
            } else {
                Set<Long> allowed = Set.copyOf(userBranchIds);
                branches = branches.stream()
                        .filter(b -> b != null && b.getId() != null && allowed.contains(b.getId()))
                        .toList();
            }
        }

        List<BranchMaster> filteredBranches = branches;
        List<com.impactsure.sanctionui.entities.Course> courses = courseRepository.findAll();
        List<com.impactsure.sanctionui.entities.BatchMaster> batches = batchMasterService.getAllBatches();
        if (!isSuperAdmin && !isHo) {
            if (userCourseIds == null || userCourseIds.isEmpty()) {
                courses = List.of();
            } else {
                courses = courses.stream()
                        .filter(c -> c != null && c.getCourseId() != null && userCourseIds.contains(c.getCourseId()))
                        .toList();
            }
            if (userBatchIds == null || userBatchIds.isEmpty()) {
                batches = List.of();
            } else {
                batches = batches.stream()
                        .filter(b -> b != null && b.getBatchId() != null && userBatchIds.contains(b.getBatchId()))
                        .toList();
            }
        }

        model.addAttribute("courses", courses);
        model.addAttribute("academicYears", academicYearRepository.findAll());
        model.addAttribute("batches", batches);
        model.addAttribute("branches", filteredBranches);
        model.addAttribute("paymentModes", paymentModes);
        model.addAttribute("active", "fees-ledger");

        return "fees-ledger";
    }

    @GetMapping("/fees-ledger/api/search")
    @ResponseBody
    public ResponseEntity<FeeLedgerResponseDto> searchFeesLedger(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String batch,
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "DUE") String dateType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dueStatus,
            @RequestParam(required = false) String paymentMode,
            @RequestParam(required = false) String verification,
            @RequestParam(required = false) String proofAttached,
            @RequestParam(required = false) String txnPresent,
            @RequestParam(required = false) String paidAmountOp,
            @RequestParam(required = false) BigDecimal paidAmount,
            @RequestParam(required = false) BigDecimal pendingMin,
            @RequestParam(required = false) BigDecimal pendingMax,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
            @AuthenticationPrincipal OidcUser oidcUser
    ) {
        String accessToken = client.getAccessToken().getTokenValue();
        boolean isSuperAdmin = hasRole(oidcUser, "SUPER_ADMIN");
        boolean isHo = hasRole(oidcUser, "HO");
        List<Long> userBranchIds = (isSuperAdmin || isHo) ? List.of() : resolveUserBranchIds(oidcUser);
        List<Long> userCourseIds = (isSuperAdmin || isHo) ? List.of() : resolveUserCourseIds(oidcUser);
        List<Long> userBatchIds = (isSuperAdmin || isHo) ? List.of() : resolveUserBatchIds(oidcUser);
        List<String> userBatchCodes = (isSuperAdmin || isHo) ? List.of() : resolveBatchCodes(userBatchIds);
        List<Long> branchIdsFilter = null;
        if (!isSuperAdmin && !isHo) {
            if (userBranchIds == null || userBranchIds.isEmpty()) {
                return ResponseEntity.ok(emptyLedger(page, size));
            }
            if (branchId != null && !userBranchIds.contains(branchId)) {
                return ResponseEntity.ok(emptyLedger(page, size));
            }
            if (branchId == null && userBranchIds.size() == 1) {
                branchId = userBranchIds.get(0);
            }
            if (branchId == null && userBranchIds.size() > 1) {
                branchIdsFilter = userBranchIds;
            }
        }

        List<Long> courseIdsFilter = null;
        String batchFilter = batch;
        List<String> batchCodesFilter = null;
        if (!isSuperAdmin && !isHo) {
            if (userCourseIds == null || userCourseIds.isEmpty()
                    || userBatchCodes == null || userBatchCodes.isEmpty()) {
                return ResponseEntity.ok(emptyLedger(page, size));
            }
            if (courseId != null) {
                if (!userCourseIds.contains(courseId)) {
                    return ResponseEntity.ok(emptyLedger(page, size));
                }
            } else {
                courseIdsFilter = userCourseIds;
            }
            if (batch != null && !batch.isBlank()) {
                if (!userBatchCodes.contains(batch)) {
                    return ResponseEntity.ok(emptyLedger(page, size));
                }
            } else {
                batchFilter = null;
                batchCodesFilter = userBatchCodes;
            }
        }

        FeeLedgerResponseDto ledger = feeLedgerClientService.searchLedger(
                page, size, q, branchId, branchIdsFilter, courseId, courseIdsFilter, batchFilter, batchCodesFilter, academicYearId,
                startDate, endDate, dateType, status, dueStatus, paymentMode,
                verification, proofAttached, txnPresent, paidAmountOp, paidAmount,
                pendingMin, pendingMax,
                accessToken
        );

        return ResponseEntity.ok(ledger);
    }

    @GetMapping("/fees-ledger/admission/{admissionId}")
    @ResponseBody
    public ResponseEntity<?> admissionLedger(
            @PathVariable Long admissionId,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
            @AuthenticationPrincipal OidcUser oidcUser
    ) {
        String accessToken = client.getAccessToken().getTokenValue();
        boolean isSuperAdmin = hasRole(oidcUser, "SUPER_ADMIN");
        boolean isHo = hasRole(oidcUser, "HO");
        List<Long> userBranchIds = (isSuperAdmin || isHo) ? List.of() : resolveUserBranchIds(oidcUser);
        List<Long> userCourseIds = (isSuperAdmin || isHo) ? List.of() : resolveUserCourseIds(oidcUser);
        List<Long> userBatchIds = (isSuperAdmin || isHo) ? List.of() : resolveUserBatchIds(oidcUser);
        List<String> userBatchCodes = (isSuperAdmin || isHo) ? List.of() : resolveBatchCodes(userBatchIds);
        Admission2 admission = admissionApiClientService.getAdmissionById(admissionId);
        if (!isSuperAdmin && !isHo && !isAdmissionInBranches(admission, userBranchIds)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (!isSuperAdmin && !isHo && !isAdmissionInCourses(admission, userCourseIds)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (!isSuperAdmin && !isHo && !isAdmissionInBatches(admission, userBatchCodes)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Map<Long, FileUpload> receipts = admissionApiClientService.findReceiptMapForAdmission(admissionId);
        Map<Long, List<FeeInstallmentPaymentDto>> payments = new LinkedHashMap<>();
        List<FeeInstallment> installments = admission.getInstallments();
        if (installments != null) {
            for (FeeInstallment installment : installments) {
                if (installment == null || installment.getInstallmentId() == null) {
                    continue;
                }
                List<FeeInstallmentPaymentDto> items = admissionApiClientService.getInstallmentPayments(
                        installment.getInstallmentId(),
                        accessToken
                );
                payments.put(installment.getInstallmentId(), items);
            }
        }
        return ResponseEntity.ok(Map.of(
                "admission", admission,
                "receipts", receipts,
                "payments", payments
        ));
    }

    @GetMapping("/fees-ledger/comments/student/{studentId}")
    @ResponseBody
    public ResponseEntity<List<StudentFeeCommentDto>> getStudentComments(
            @PathVariable Long studentId,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client
    ) {
        String accessToken = client.getAccessToken().getTokenValue();
        List<StudentFeeCommentDto> comments = studentFeeCommentClientService.getCommentsByStudentId(studentId, accessToken);
        return ResponseEntity.ok(comments);
    }

    @PostMapping("/fees-ledger/comments")
    @ResponseBody
    public ResponseEntity<StudentFeeCommentDto> createComment(
            @RequestBody CreateStudentFeeCommentRequest request,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client
    ) {
        String accessToken = client.getAccessToken().getTokenValue();
        StudentFeeCommentDto created = studentFeeCommentClientService.createComment(request, accessToken);
        return ResponseEntity.ok(created);
    }

    @DeleteMapping("/fees-ledger/comments/{commentId}")
    @ResponseBody
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client
    ) {
        String accessToken = client.getAccessToken().getTokenValue();
        studentFeeCommentClientService.deleteComment(commentId, accessToken);
        return ResponseEntity.noContent().build();
    }

    // Schedule endpoints
    @GetMapping("/fees-ledger/schedules/student/{studentId}")
    @ResponseBody
    public ResponseEntity<List<StudentFeeScheduleDto>> getStudentSchedules(
            @PathVariable Long studentId,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client
    ) {
        String accessToken = client.getAccessToken().getTokenValue();
        List<StudentFeeScheduleDto> schedules = studentFeeScheduleClientService.getSchedulesByStudentId(studentId, accessToken);
        return ResponseEntity.ok(schedules);
    }

    @GetMapping("/fees-ledger/schedules/student/{studentId}/pending")
    @ResponseBody
    public ResponseEntity<List<StudentFeeScheduleDto>> getPendingStudentSchedules(
            @PathVariable Long studentId,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client
    ) {
        String accessToken = client.getAccessToken().getTokenValue();
        List<StudentFeeScheduleDto> schedules = studentFeeScheduleClientService.getPendingSchedulesByStudentId(studentId, accessToken);
        return ResponseEntity.ok(schedules);
    }

    @PostMapping("/fees-ledger/schedules")
    @ResponseBody
    public ResponseEntity<StudentFeeScheduleDto> createSchedule(
            @RequestBody CreateStudentFeeScheduleRequest request,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client
    ) {
        String accessToken = client.getAccessToken().getTokenValue();
        StudentFeeScheduleDto created = studentFeeScheduleClientService.createSchedule(request, accessToken);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/fees-ledger/schedules/{scheduleId}/status")
    @ResponseBody
    public ResponseEntity<StudentFeeScheduleDto> updateScheduleStatus(
            @PathVariable Long scheduleId,
            @RequestParam String status,
            @RequestParam(required = false) String completedBy,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client
    ) {
        String accessToken = client.getAccessToken().getTokenValue();
        StudentFeeScheduleDto updated = studentFeeScheduleClientService.updateScheduleStatus(scheduleId, status, completedBy, accessToken);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/fees-ledger/schedules/{scheduleId}")
    @ResponseBody
    public ResponseEntity<Void> deleteSchedule(
            @PathVariable Long scheduleId,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client
    ) {
        String accessToken = client.getAccessToken().getTokenValue();
        studentFeeScheduleClientService.deleteSchedule(scheduleId, accessToken);
        return ResponseEntity.noContent().build();
    }

    private boolean hasRole(OidcUser oidcUser, String role) {
        if (oidcUser == null || role == null) {
            return false;
        }
        String expected = "ROLE_" + role;
        return oidcUser.getAuthorities().stream()
                .anyMatch(a -> expected.equals(a.getAuthority()));
    }

    private List<Long> resolveUserBranchIds(OidcUser oidcUser) {
        if (oidcUser == null || oidcUser.getSubject() == null) {
            return List.of();
        }
        return userBranchMappingService.getBranchIds(oidcUser.getSubject());
    }

    private List<Long> resolveUserCourseIds(OidcUser oidcUser) {
        if (oidcUser == null || oidcUser.getSubject() == null) {
            return List.of();
        }
        return userCourseMappingService.getCourseIds(oidcUser.getSubject());
    }

    private List<Long> resolveUserBatchIds(OidcUser oidcUser) {
        if (oidcUser == null || oidcUser.getSubject() == null) {
            return List.of();
        }
        return userBatchMappingService.getBatchIds(oidcUser.getSubject());
    }

    private List<String> resolveBatchCodes(List<Long> batchIds) {
        if (batchIds == null || batchIds.isEmpty()) {
            return List.of();
        }
        Map<Long, String> byId = batchMasterService.getAllBatches().stream()
                .filter(b -> b != null && b.getBatchId() != null)
                .collect(java.util.stream.Collectors.toMap(b -> b.getBatchId(), b -> b.getCode(), (a, b) -> a));
        return batchIds.stream()
                .map(byId::get)
                .filter(c -> c != null && !c.isBlank())
                .toList();
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

    private boolean isAdmissionInCourses(Admission2 admission, List<Long> courseIds) {
        if (admission == null || courseIds == null || courseIds.isEmpty()) {
            return false;
        }
        if (admission.getCourse() == null || admission.getCourse().getCourseId() == null) {
            return false;
        }
        return courseIds.contains(admission.getCourse().getCourseId());
    }

    private boolean isAdmissionInBatches(Admission2 admission, List<String> batchCodes) {
        if (admission == null || batchCodes == null || batchCodes.isEmpty()) {
            return false;
        }
        String batch = admission.getBatch();
        return batch != null && batchCodes.contains(batch);
    }

    private FeeLedgerResponseDto emptyLedger(int page, int size) {
        FeeLedgerResponseDto empty = new FeeLedgerResponseDto();
        empty.setContent(List.of());
        empty.setPage(page);
        empty.setSize(size);
        empty.setTotalElements(0);
        empty.setTotalPages(0);
        return empty;
    }
}
