package com.impactsure.sanctionui.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestTemplate;

import com.impactsure.sanctionui.dto.FeeLedgerResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeeLedgerClientService {

    private final RestTemplate restTemplate;

    @Value("${admission.service.url}")
    private String baseUrl;

    public FeeLedgerResponseDto searchLedger(
            int page,
            int size,
            String q,
            Long branchId,
            List<Long> branchIds,
            Long courseId,
            List<Long> courseIds,
            String batch,
            List<String> batchCodes,
            Long academicYearId,
            LocalDate startDate,
            LocalDate endDate,
            String dateType,
            String status,
            String dueStatus,
            String paymentMode,
            String verification,
            String proofAttached,
            String txnPresent,
            String paidAmountOp,
            BigDecimal paidAmount,
            BigDecimal pendingMin,
            BigDecimal pendingMax,
            String accessToken
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/api/fees/ledger")
                .queryParam("page", page)
                .queryParam("size", size);

        if (q != null && !q.isBlank()) builder.queryParam("q", q);
        if (branchId != null) builder.queryParam("branchId", branchId);
        if (branchId == null && branchIds != null && !branchIds.isEmpty()) {
            String csv = branchIds.stream()
                    .filter(id -> id != null)
                    .map(String::valueOf)
                    .reduce((a, b) -> a + "," + b)
                    .orElse(null);
            if (csv != null && !csv.isBlank()) {
                builder.queryParam("branchIds", csv);
            }
        }
        if (courseId != null) builder.queryParam("courseId", courseId);
        if (courseId == null && courseIds != null && !courseIds.isEmpty()) {
            String csv = courseIds.stream()
                    .filter(id -> id != null)
                    .map(String::valueOf)
                    .reduce((a, b) -> a + "," + b)
                    .orElse(null);
            if (csv != null && !csv.isBlank()) {
                builder.queryParam("courseIds", csv);
            }
        }
        if (batch != null && !batch.isBlank()) builder.queryParam("batch", batch);
        if ((batch == null || batch.isBlank()) && batchCodes != null && !batchCodes.isEmpty()) {
            String csv = batchCodes.stream()
                    .filter(code -> code != null && !code.isBlank())
                    .map(String::trim)
                    .reduce((a, b) -> a + "," + b)
                    .orElse(null);
            if (csv != null && !csv.isBlank()) {
                builder.queryParam("batchCodes", csv);
            }
        }
        if (academicYearId != null) builder.queryParam("academicYearId", academicYearId);
        if (startDate != null) builder.queryParam("startDate", startDate);
        if (endDate != null) builder.queryParam("endDate", endDate);
        if (dateType != null && !dateType.isBlank()) builder.queryParam("dateType", dateType);
        if (status != null && !status.isBlank()) builder.queryParam("status", status);
        if (dueStatus != null && !dueStatus.isBlank()) builder.queryParam("dueStatus", dueStatus);
        if (paymentMode != null && !paymentMode.isBlank()) builder.queryParam("paymentMode", paymentMode);
        if (verification != null && !verification.isBlank()) builder.queryParam("verification", verification);
        if (proofAttached != null && !proofAttached.isBlank()) builder.queryParam("proofAttached", proofAttached);
        if (txnPresent != null && !txnPresent.isBlank()) builder.queryParam("txnPresent", txnPresent);
        if (paidAmountOp != null && !paidAmountOp.isBlank()) builder.queryParam("paidAmountOp", paidAmountOp);
        if (paidAmount != null) builder.queryParam("paidAmount", paidAmount);
        if (pendingMin != null) builder.queryParam("pendingMin", pendingMin);
        if (pendingMax != null) builder.queryParam("pendingMax", pendingMax);

        String url = builder.toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<FeeLedgerResponseDto> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody();
    }
}
