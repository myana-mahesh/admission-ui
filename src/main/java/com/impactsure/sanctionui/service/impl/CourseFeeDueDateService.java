package com.impactsure.sanctionui.service.impl;


import com.impactsure.sanctionui.dto.CourseFeeRequestDto;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class CourseFeeDueDateService {

    public static CourseFeeRequestDto applyDueDates(CourseFeeRequestDto dto, LocalDate today) {
        if (dto == null || dto.getFeeTemplate() == null) return null;
        List<CourseFeeRequestDto.InstallmentDto> list = dto.getFeeTemplate().getInstallments();
        if (list == null || list.isEmpty()) return null;

        // Find Year-1 first installment (min sequence among yearNumber=1)
        CourseFeeRequestDto.InstallmentDto firstYearFirst = list.stream()
                .filter(x -> x.getYearNumber() != null && x.getYearNumber() == 1)
                .min(Comparator.comparingInt(x -> x.getSequence() == null ? Integer.MAX_VALUE : x.getSequence()))
                .orElse(null);

        if (firstYearFirst == null) {
            // fallback: overall min sequence
            firstYearFirst = list.stream()
                    .min(Comparator.comparingInt(x -> x.getSequence() == null ? Integer.MAX_VALUE : x.getSequence()))
                    .orElse(null);
            if (firstYearFirst == null) return null;
        }

        int anchorMonth = safeMonth(firstYearFirst.getDueMonth());
        int anchorDay = safeDay(firstYearFirst.getDueDayOfMonth());

        LocalDate candidateThisYear = safeDate(today.getYear(), anchorMonth, anchorDay);
        int baseYear = !candidateThisYear.isBefore(today) ? today.getYear() : today.getYear() + 1;

        // Set dueDate into each installment
        for (CourseFeeRequestDto.InstallmentDto inst : list) {
            Integer yn = inst.getYearNumber();
            if (yn == null || yn <= 0) yn = 1;

            int dueYear = baseYear + (yn - 1);
            int m = safeMonth(inst.getDueMonth());
            int d = safeDay(inst.getDueDayOfMonth());

            inst.setDueDate(safeDate(dueYear, m, d));
        }
        return dto;
    }

    private static int safeMonth(Integer m) {
        if (m == null || m < 1 || m > 12) return 1;
        return m;
    }

    private static int safeDay(Integer d) {
        if (d == null || d < 1 || d > 31) return 1;
        return d;
    }

    // Clamp invalid day for the month (e.g., 31-Apr -> 30-Apr)
    private static LocalDate safeDate(int year, int month, int day) {
        YearMonth ym = YearMonth.of(year, month);
        int clamped = Math.min(day, ym.lengthOfMonth());
        return LocalDate.of(year, month, clamped);
    }
}


