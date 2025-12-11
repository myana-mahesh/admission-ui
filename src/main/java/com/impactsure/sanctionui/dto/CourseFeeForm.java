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

    @Builder.Default
    private List<Integer> installmentDueDays = new ArrayList<>();

    public BigDecimal getComputedTotal() {
        return installmentAmounts.stream()
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    @Builder.Default
    private List<Integer> installmentYears = new ArrayList<>();

}

