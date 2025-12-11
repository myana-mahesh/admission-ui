package com.impactsure.sanctionui.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "course",
       indexes = { @Index(name = "uk_course_code", columnList = "code", unique = true) })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
 public class Course extends Auditable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long courseId;

    @Column(length = 32, unique = true)
    private String code; // e.g., DPHARM

    @Column(length = 120, nullable = false)
    private String name;
    
    private Integer years;
}
