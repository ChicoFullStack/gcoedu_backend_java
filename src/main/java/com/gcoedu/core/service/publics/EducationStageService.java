package com.gcoedu.core.service.publics;

import com.gcoedu.core.domain.dto.publics.EducationStageDTO;
import com.gcoedu.core.domain.entity.publics.EducationStage;
import com.gcoedu.core.domain.entity.tenant.School;
import com.gcoedu.core.domain.entity.tenant.SchoolCourse;
import com.gcoedu.core.repository.publics.EducationStageRepository;
import com.gcoedu.core.repository.tenant.SchoolCourseRepository;
import com.gcoedu.core.repository.tenant.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EducationStageService {

    private final EducationStageRepository educationStageRepository;
    private final SchoolCourseRepository schoolCourseRepository;
    private final SchoolRepository schoolRepository;

    public List<EducationStageDTO> findAll() {
        return educationStageRepository.findAll().stream()
                .map(stage -> {
                    EducationStageDTO dto = new EducationStageDTO();
                    dto.setId(stage.getId());
                    dto.setName(stage.getName());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public List<EducationStageDTO> findBySchoolId(String schoolId) {
        return schoolCourseRepository.findBySchoolId(schoolId).stream()
                .map(SchoolCourse::getEducationStage)
                .filter(Objects::nonNull)
                .map(stage -> {
                    EducationStageDTO dto = new EducationStageDTO();
                    dto.setId(stage.getId());
                    dto.setName(stage.getName());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void linkCoursesToSchool(String schoolId, List<UUID> courseIds) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Escola não encontrada"));

        for (UUID courseId : courseIds) {
            Optional<EducationStage> stageOpt = educationStageRepository.findById(courseId);
            if (stageOpt.isPresent()) {
                boolean exists = schoolCourseRepository.findBySchoolId(schoolId).stream()
                        .anyMatch(sc -> sc.getEducationStage() != null && sc.getEducationStage().getId().equals(courseId));
                if (!exists) {
                    SchoolCourse sc = new SchoolCourse();
                    sc.setSchool(school);
                    sc.setEducationStage(stageOpt.get());
                    schoolCourseRepository.save(sc);
                }
            }
        }
    }

    @Transactional
    public void unlinkCourseFromSchool(String schoolId, UUID courseId) {
        List<SchoolCourse> schoolCourses = schoolCourseRepository.findBySchoolId(schoolId);
        for (SchoolCourse sc : schoolCourses) {
            if (sc.getEducationStage() != null && sc.getEducationStage().getId().equals(courseId)) {
                schoolCourseRepository.delete(sc);
            }
        }
    }
}
