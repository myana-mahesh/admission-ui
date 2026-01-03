package com.impactsure.sanctionui.repository;


import org.springframework.data.jpa.domain.Specification;

import com.impactsure.sanctionui.entities.Admission2;
import com.impactsure.sanctionui.entities.FileUpload;
import com.impactsure.sanctionui.entities.StudentOtherPaymentValue;
import com.impactsure.sanctionui.dto.OtherPaymentFilterDto;
import com.impactsure.sanctionui.enums.Gender;
import com.impactsure.sanctionui.enums.AdmissionStatus;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class AdmissionSpecifications {

    private AdmissionSpecifications() {}

    /** Filter by admission status list */
    public static Specification<Admission2> statusIn(Collection<AdmissionStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) return null;
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    /**
     * Case-insensitive keyword search across:
     * - Admission: formNo, remarks, lastCollege, collegeAttended, collegeLocation
     * - Student: fullName, mobile, aadhaar, email
     * - Course: name, code
     * - AcademicYear: label
     */
    public static Specification<Admission2> keywordLike(String q) {
        if (q == null || q.trim().isEmpty()) return null;
        String like = "%" + q.trim().toLowerCase() + "%";

        return (root, query, cb) -> {
            var student = root.join("student", JoinType.LEFT);
            var course = root.join("course", JoinType.LEFT);
            var year = root.join("year", JoinType.LEFT);

            return cb.or(
                cb.like(cb.lower(root.get("formNo")), like),
                cb.like(cb.lower(root.get("remarks")), like),
                cb.like(cb.lower(root.get("lastCollege")), like),
                cb.like(cb.lower(root.get("collegeAttended")), like),
                cb.like(cb.lower(root.get("collegeLocation")), like),

                cb.like(cb.lower(student.get("fullName")), like),
                cb.like(cb.lower(student.get("mobile")), like),
                cb.like(cb.lower(student.get("aadhaar")), like),
                cb.like(cb.lower(student.get("email")), like),

                cb.like(cb.lower(course.get("name")), like),
                cb.like(cb.lower(course.get("code")), like),

                cb.like(cb.lower(year.get("label")), like)
            );
        };
    }

    public static Specification<Admission2> courseIdEquals(Long courseId) {
        if (courseId == null) return null;
        return (root, query, cb) -> cb.equal(root.join("course", JoinType.LEFT).get("courseId"), courseId);
    }

    public static Specification<Admission2> courseIdIn(Collection<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) return null;
        return (root, query, cb) -> root.join("course", JoinType.LEFT).get("courseId").in(courseIds);
    }

    public static Specification<Admission2> collegeIdEquals(Long collegeId) {
        if (collegeId == null) return null;
        return (root, query, cb) -> cb.equal(root.join("college", JoinType.LEFT).get("collegeId"), collegeId);
    }

    public static Specification<Admission2> batchEquals(String batch) {
        if (batch == null || batch.isBlank()) return null;
        return (root, query, cb) -> cb.equal(root.get("batch"), batch.trim());
    }

    public static Specification<Admission2> yearIdEquals(Long yearId) {
        if (yearId == null) return null;
        return (root, query, cb) -> cb.equal(root.join("year", JoinType.LEFT).get("yearId"), yearId);
    }

    public static Specification<Admission2> genderEquals(Gender gender) {
        if (gender == null) return null;
        return (root, query, cb) -> cb.equal(root.join("student", JoinType.LEFT).get("gender"), gender);
    }

    public static Specification<Admission2> studentIdIn(Collection<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) return null;
        return (root, query, cb) -> root.join("student", JoinType.LEFT).get("studentId").in(studentIds);
    }

    public static Specification<Admission2> admissionBranchIdIn(Collection<Long> branchIds) {
        if (branchIds == null || branchIds.isEmpty()) return null;
        return (root, query, cb) -> root.join("admissionBranch", JoinType.LEFT).get("id").in(branchIds);
    }

    public static Specification<Admission2> branchApproved(Boolean approved) {
        if (approved == null) return null;
        return (root, query, cb) -> cb.equal(root.get("branchApproved"), approved);
    }

    public static Specification<Admission2> documentsReceived(Collection<Long> docTypeIds, Boolean received) {
        if (docTypeIds == null || docTypeIds.isEmpty() || received == null) return null;
        return (root, query, cb) -> {
            query.distinct(true);

            List<Predicate> perDocPredicates = new ArrayList<>();
            for (Long docTypeId : docTypeIds) {
                if (docTypeId == null) {
                    continue;
                }
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<FileUpload> upload = subquery.from(FileUpload.class);
                subquery.select(cb.literal(1L));
                subquery.where(
                        cb.equal(upload.get("admission").get("admissionId"), root.get("admissionId")),
                        cb.equal(upload.get("docType").get("docTypeId"), docTypeId)
                );
                Predicate exists = cb.exists(subquery);
                perDocPredicates.add(received ? exists : cb.not(exists));
            }

            if (perDocPredicates.isEmpty()) {
                return null;
            }
            return received
                    ? cb.and(perDocPredicates.toArray(new Predicate[0]))
                    : cb.or(perDocPredicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Admission2> otherPaymentFilters(List<OtherPaymentFilterDto> filters) {
        if (filters == null || filters.isEmpty()) return null;
        return (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> all = new ArrayList<>();
            for (OtherPaymentFilterDto filter : filters) {
                if (filter == null || filter.getFieldId() == null) {
                    continue;
                }
                String inputType = filter.getInputType() == null ? "" : filter.getInputType().toLowerCase(Locale.ROOT);
                String operator = filter.getOperator() == null ? "" : filter.getOperator().toLowerCase(Locale.ROOT);
                String value = filter.getValue() != null ? filter.getValue().trim() : null;
                List<String> values = filter.getValues();

                Subquery<Long> subquery = query.subquery(Long.class);
                Root<StudentOtherPaymentValue> v = subquery.from(StudentOtherPaymentValue.class);
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(v.get("student").get("studentId"), root.get("student").get("studentId")));
                predicates.add(cb.equal(v.get("field").get("id"), filter.getFieldId()));

                if ("select".equals(inputType) || "radio".equals(inputType)) {
                    if (value == null || value.isBlank()) {
                        continue;
                    }
                    predicates.add(cb.equal(v.get("option").get("id"), Long.valueOf(value)));
                } else if ("checkbox".equals(inputType)) {
                    if (values == null || values.isEmpty()) {
                        continue;
                    }
                    List<Long> optionIds = values.stream()
                            .filter(val -> val != null && !val.isBlank())
                            .map(Long::valueOf)
                            .toList();
                    if (optionIds.isEmpty()) {
                        continue;
                    }
                    predicates.add(v.get("option").get("id").in(optionIds));
                } else if ("number".equals(inputType)) {
                    if (value == null || value.isBlank()) {
                        continue;
                    }
                    try {
                        Double num = Double.valueOf(value);
                        var numericValue = cb.toDouble(v.get("value"));
                        predicates.add(cb.isNotNull(v.get("value")));
                        predicates.add(compareNumeric(cb, numericValue, num, operator));
                    } catch (NumberFormatException ex) {
                        continue;
                    }
                } else if ("date".equals(inputType)) {
                    if (value == null || value.isBlank()) {
                        continue;
                    }
                    var dateValue = cb.function("str_to_date", java.time.LocalDate.class, v.get("value"), cb.literal("%Y-%m-%d"));
                    predicates.add(cb.isNotNull(v.get("value")));
                    if ("before".equals(operator)) {
                        predicates.add(cb.lessThan(dateValue, java.time.LocalDate.parse(value)));
                    } else if ("after".equals(operator)) {
                        predicates.add(cb.greaterThan(dateValue, java.time.LocalDate.parse(value)));
                    } else {
                        predicates.add(cb.equal(dateValue, java.time.LocalDate.parse(value)));
                    }
                } else {
                    if (value == null || value.isBlank()) {
                        continue;
                    }
                    String like = "%" + value.toLowerCase(Locale.ROOT) + "%";
                    if ("equals".equals(operator)) {
                        predicates.add(cb.equal(cb.lower(v.get("value")), value.toLowerCase(Locale.ROOT)));
                    } else {
                        predicates.add(cb.like(cb.lower(v.get("value")), like));
                    }
                }

                subquery.select(cb.literal(1L));
                subquery.where(predicates.toArray(new Predicate[0]));
                all.add(cb.exists(subquery));
            }

            return all.isEmpty() ? null : cb.and(all.toArray(new Predicate[0]));
        };
    }

    private static Predicate compareNumeric(jakarta.persistence.criteria.CriteriaBuilder cb,
            jakarta.persistence.criteria.Expression<Double> path,
            Double value,
            String operator) {
        if ("gt".equals(operator)) {
            return cb.greaterThan(path, value);
        }
        if ("gte".equals(operator)) {
            return cb.greaterThanOrEqualTo(path, value);
        }
        if ("lt".equals(operator)) {
            return cb.lessThan(path, value);
        }
        if ("lte".equals(operator)) {
            return cb.lessThanOrEqualTo(path, value);
        }
        return cb.equal(path, value);
    }
}
