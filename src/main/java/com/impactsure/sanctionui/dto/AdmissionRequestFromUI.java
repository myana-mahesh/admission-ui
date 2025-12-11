package com.impactsure.sanctionui.dto;

import java.time.LocalDate;

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
	
	private Long course;
	
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
    private String studentId;
    private Integer installmentsCount;
	

}
