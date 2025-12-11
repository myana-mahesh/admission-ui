package com.impactsure.sanctionui.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.impactsure.sanctionui.entities.Course;


@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
	  Optional<Course> findByCode(String code);

}
