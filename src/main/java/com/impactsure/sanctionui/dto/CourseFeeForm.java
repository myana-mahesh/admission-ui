package com.impactsure.sanctionui.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseFeeForm {

    private Long courseId;
    private String code;
    private String name;
    private Integer years;

    private Long templateId;
    private String templateName;

    // Installment fields as parallel lists (same index = same row)
    @Builder.Default
    private List<Long> installmentIds = new ArrayList<>();

    @Builder.Default
    private List<Integer> installmentSequences = new ArrayList<>();

    @Builder.Default
    private List<BigDecimal> installmentAmounts = new ArrayList<>();

    /**
     * IMPORTANT: previously it was "Due Days from Admission"
     * Now we are using it as "Due Day of Month" (1..31)
     */
    @Builder.Default
    private List<Integer> installmentDueDays = new ArrayList<>();

    /**
     * NEW: Due Month (1..12)
     * This + installmentDueDays => due date every year
     */
    @Builder.Default
    private List<Integer> installmentDueMonths = new ArrayList<>();

    @Builder.Default
    private List<Integer> installmentYears = new ArrayList<>();

    public BigDecimal getComputedTotal() {
        return installmentAmounts.stream()
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Optional helper: ensures all lists have at least 'size' elements.
     * Call this before rendering or before saving (controller/service),
     * so Thymeleaf doesn't crash when a list is missing an index.
     */
    public void ensureSize(int size) {
        while (installmentIds.size() < size) installmentIds.add(null);
        while (installmentSequences.size() < size) installmentSequences.add(null);
        while (installmentAmounts.size() < size) installmentAmounts.add(null);
        while (installmentDueDays.size() < size) installmentDueDays.add(1);     // default day = 1
        while (installmentDueMonths.size() < size) installmentDueMonths.add(1); // default month = Jan
        while (installmentYears.size() < size) installmentYears.add(1);         // default year = 1
    }
}
