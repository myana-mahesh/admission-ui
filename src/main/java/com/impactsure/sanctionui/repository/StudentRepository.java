package com.impactsure.sanctionui.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.impactsure.sanctionui.entities.Student;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
	  Optional<Student> findByAbsId(String absId);
	  Optional<Student> findByAadhaar(String aadhaar);
	  Optional<Student> findByMobile(String mobile);
	  @Query("select s from Student s where lower(s.fullName) like lower(concat('%', :q, '%'))")
	  List<Student> searchByName(String q);
	Student findByAbsIdOrMobile(String absId, String mobile);
	}
