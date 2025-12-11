package com.impactsure.sanctionui.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.impactsure.sanctionui.entities.Admission;


public interface AdmissionRepository extends JpaRepository<Admission, Long> {
    boolean existsByCourseAndBranch(String course, String branch);
}
