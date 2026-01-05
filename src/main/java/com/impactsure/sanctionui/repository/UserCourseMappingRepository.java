package com.impactsure.sanctionui.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import com.impactsure.sanctionui.entities.UserCourseMapping;

public interface UserCourseMappingRepository extends JpaRepository<UserCourseMapping, Long> {
    List<UserCourseMapping> findByUserId(String userId);

    @Modifying
    @Transactional
    void deleteByUserId(String userId);
}
