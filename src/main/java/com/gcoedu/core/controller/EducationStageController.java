package com.gcoedu.core.controller;

import com.gcoedu.core.domain.dto.publics.EducationStageDTO;
import com.gcoedu.core.service.publics.EducationStageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class EducationStageController {

    private final EducationStageService educationStageService;

    @GetMapping(value = {"/education_stages", "/api/education_stages", "/education_stages/", "/api/education_stages/"})
    @PreAuthorize("hasAnyRole('ADMIN', 'TECADM', 'MANAGER', 'TEACHER')")
    public ResponseEntity<List<EducationStageDTO>> getEducationStages() {
        return ResponseEntity.ok(educationStageService.findAll());
    }

    @GetMapping(value = {"/education_stages/all", "/api/education_stages/all"})
    @PreAuthorize("hasAnyRole('ADMIN', 'TECADM', 'MANAGER', 'TEACHER')")
    public ResponseEntity<List<EducationStageDTO>> getAllEducationStages() {
        return ResponseEntity.ok(educationStageService.findAll());
    }

    @GetMapping(value = {"/school/{schoolId}/courses", "/api/school/{schoolId}/courses"})
    @PreAuthorize("hasAnyRole('ADMIN', 'TECADM', 'MANAGER', 'TEACHER')")
    public ResponseEntity<Map<String, Object>> getCoursesBySchool(@PathVariable String schoolId) {
        List<EducationStageDTO> courses = educationStageService.findBySchoolId(schoolId);
        Map<String, Object> response = new HashMap<>();
        response.put("school_id", schoolId);
        response.put("courses", courses);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = {"/school/{schoolId}/courses", "/api/school/{schoolId}/courses"})
    @PreAuthorize("hasAnyRole('ADMIN', 'TECADM', 'MANAGER')")
    public ResponseEntity<Void> linkCoursesToSchool(@PathVariable String schoolId, @RequestBody Map<String, Object> body) {
        List<UUID> courseIds = new ArrayList<>();
        if (body.containsKey("education_stage_ids")) {
            List<?> ids = (List<?>) body.get("education_stage_ids");
            for (Object id : ids) {
                courseIds.add(UUID.fromString(id.toString()));
            }
        } else if (body.containsKey("education_stage_id")) {
            courseIds.add(UUID.fromString(body.get("education_stage_id").toString()));
        }

        educationStageService.linkCoursesToSchool(schoolId, courseIds);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = {"/school/{schoolId}/courses/{courseId}", "/api/school/{schoolId}/courses/{courseId}"})
    @PreAuthorize("hasAnyRole('ADMIN', 'TECADM', 'MANAGER')")
    public ResponseEntity<Void> unlinkCourseFromSchool(@PathVariable String schoolId, @PathVariable UUID courseId) {
        educationStageService.unlinkCourseFromSchool(schoolId, courseId);
        return ResponseEntity.ok().build();
    }
}
