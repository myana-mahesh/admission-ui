package com.impactsure.sanctionui.repository;


import org.springframework.data.jpa.domain.Specification;

import com.impactsure.sanctionui.entities.Admission2;
import com.impactsure.sanctionui.enums.AdmissionStatus;

import jakarta.persistence.criteria.JoinType;

import java.util.Collection;

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
}

