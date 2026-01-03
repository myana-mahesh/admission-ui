package com.impactsure.sanctionui.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.impactsure.sanctionui.entities.Admission2;


@Repository
public interface Admission2Repository extends JpaRepository<Admission2, Long> , JpaSpecificationExecutor<Admission2>{
	  @EntityGraph(attributePaths = {"student","course","year"})
	  Optional<Admission2> findByAdmissionId(Long id);

	  List<Admission2> findByCourseCourseIdAndYearYearId(Long courseId, Long yearId);

	  @Query("select a from Admission2 a where a.examDueDate between :from and :to")
	  List<Admission2> findExamDueBetween(LocalDate from, LocalDate to);

	  Optional<Admission2> findByStudentStudentIdAndYearYearId(Long studentId, Long yearId);

	  @EntityGraph(attributePaths = {"student","course","college","installments"})
	  Optional<Admission2> findTopByStudentStudentIdOrderByAdmissionIdDesc(Long studentId);
	  
	// choose whichever fetch strategy you prefer
	    @EntityGraph(attributePaths = {"student","course","installments"})
	    Optional<Admission2> findById(Long id);

	  @Query("select distinct a from Admission2 a " +
	           "left join fetch a.student " +
	           "left join fetch a.course " +
	           "left join fetch a.installments " +
	           "where a.admissionId = :id")
	    Optional<Admission2> findByIdWithStudentCourseInstallments(@Param("id") Long id);

	  @Query("select a from Admission2 a " +
	         "left join fetch a.student " +
	         "left join fetch a.course " +
	         "left join fetch a.college " +
	         "left join fetch a.installments " +
	         "where a.student.studentId in :studentIds " +
	         "and a.admissionId in (" +
	         "  select max(a2.admissionId) from Admission2 a2 " +
	         "  where a2.student.studentId in :studentIds " +
	         "  group by a2.student.studentId" +
	         ")")
	  List<Admission2> findLatestByStudentIds(@Param("studentIds") List<Long> studentIds);

}
