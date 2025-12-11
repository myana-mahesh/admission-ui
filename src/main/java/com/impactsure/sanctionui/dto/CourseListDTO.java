package com.impactsure.sanctionui.dto;


import java.math.BigDecimal;

public class CourseListDTO {

 private Long id;
 private String code;
 private String name;
 private Integer years;
 private String templateName;
 private Integer totalInstallments;
 private BigDecimal totalAmount;

 // getters & setters

 public Long getId() {
     return id;
 }

 public void setId(Long id) {
     this.id = id;
 }

 public String getCode() {
     return code;
 }

 public void setCode(String code) {
     this.code = code;
 }

 public String getName() {
     return name;
 }

 public void setName(String name) {
     this.name = name;
 }

 public Integer getYears() {
     return years;
 }

 public void setYears(Integer years) {
     this.years = years;
 }

 public String getTemplateName() {
     return templateName;
 }

 public void setTemplateName(String templateName) {
     this.templateName = templateName;
 }

 public Integer getTotalInstallments() {
     return totalInstallments;
 }

 public void setTotalInstallments(Integer totalInstallments) {
     this.totalInstallments = totalInstallments;
 }

 public BigDecimal getTotalAmount() {
     return totalAmount;
 }

 public void setTotalAmount(BigDecimal totalAmount) {
     this.totalAmount = totalAmount;
 }
}
