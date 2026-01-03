package com.impactsure.sanctionui.entities;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.impactsure.sanctionui.enums.Gender;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "student",
       indexes = {
           @Index(name = "idx_student_aadhaar", columnList = "aadhaar"),
           @Index(name = "idx_student_mobile", columnList = "mobile")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Student extends Auditable {
	
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studentId;

    @Column(length = 32, unique = true)
    private String absId; // ABS-000000

    @Column(length = 150, nullable = false)
    private String fullName;

    @Column(nullable = false)
    private LocalDate dob;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private Gender gender;

    @Column(length = 12)
    private String aadhaar; // digits only

    @Column(length = 64)
    private String nationality;

    @Column(length = 64)
    private String religion;

    @Column(length = 64)
    private String caste;

    @Column(length = 160)
    private String email;

    @Column(length = 20)
    private String mobile;

    @Column(length = 5)
    private String bloodGroup;
    // Relations
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Guardian> guardians = new ArrayList<>();

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudentAddress> addresses = new ArrayList<>();

    private Integer age;

    @Column(name = "batch")
    private String batch;

    @Column(name = "registration_number", unique = true)
    private String registrationNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id")
    private Course course;

    // 🔥 INVERSE SIDE
    @OneToOne(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private SscDetails sscDetails;

    @OneToOne(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private HscDetails hscDetails;

 /*   @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    private String academicYearLabel; // e.g., 2025-26*/
}
