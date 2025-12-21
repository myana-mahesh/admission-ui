package com.impactsure.sanctionui.dto;

import java.time.LocalDate;

import com.impactsure.sanctionui.entities.HscDetails;
import com.impactsure.sanctionui.entities.SscDetailsRequest;
import org.springframework.format.annotation.DateTimeFormat;

import com.impactsure.sanctionui.enums.Gender;

import lombok.Data;

@Data
public class AdmissionRequestFromUI {
	
	private String  formNo;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate formDate;
	
	
	private String fullName;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate dob;
	private Gender gender;
	
	private String aadhaar;
	
	private String email;
	private String nationality;
	private String religion;
	private String caste;
	private String mobile;
	private String absId;
	// Optional simple address fields for quick create
	private String addressLine1;
	private String city;
	private String state;
	private String pincode;
	// Optional guardians
	private String fatherName;
	private String fatherMobile;
	private String motherName;
	private String motherMobile;
	private String bloodGroup;
	private Long course;
	private Long collegeId;
	
	private String lastCollege;
	private String collegeAttended;
	private String collegeLocation;
	private String remarks;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate examDueDate;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate dateOfAdmission;
	private Double totalFees;
    
    private Double discountAmount;
    private String discountRemark;
    
    private String discountRemarkOther;
    private String studentId;
    private Integer installmentsCount;
	private SscDetailsRequest sscDetails;
	private HscDetails hscDetails;
	private String batch;
	private String registrationNumber;
	private String referenceName;
	private Long lectureBranchId;
	private Long  admissionBranchId;
	private Integer age;
/*	private Long courseCode; // e.g., DPHARM
	private String academicYearLabel; // e.g., 2025-26*/


}
