package com.impactsure.sanctionui.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.impactsure.sanctionui.entities.UserCourseMapping;
import com.impactsure.sanctionui.repository.UserCourseMappingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserCourseMappingService {

    private final UserCourseMappingRepository userCourseMappingRepository;

    public List<Long> getCourseIds(String userId) {
        return userCourseMappingRepository.findByUserId(userId)
                .stream()
                .map(UserCourseMapping::getCourseId)
                .collect(Collectors.toList());
    }

    @Transactional
    public void replaceUserCourses(String userId, List<Long> courseIds) {
        userCourseMappingRepository.deleteByUserId(userId);
        userCourseMappingRepository.flush();
        if (courseIds == null || courseIds.isEmpty()) {
            return;
        }
        List<UserCourseMapping> mappings = courseIds.stream()
                .filter(courseId -> courseId != null)
                .distinct()
                .map(courseId -> UserCourseMapping.builder()
                        .userId(userId)
                        .courseId(courseId)
                        .build())
                .toList();
        userCourseMappingRepository.saveAll(mappings);
    }
}
