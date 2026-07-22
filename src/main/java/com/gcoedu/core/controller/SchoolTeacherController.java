package com.gcoedu.core.controller;

import com.gcoedu.core.domain.entity.tenant.School;
import com.gcoedu.core.domain.entity.tenant.SchoolTeacher;
import com.gcoedu.core.domain.entity.tenant.Teacher;
import com.gcoedu.core.repository.tenant.SchoolRepository;
import com.gcoedu.core.repository.tenant.SchoolTeacherRepository;
import com.gcoedu.core.repository.tenant.TeacherRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping(value = {"/api/v1/tenant/school-teacher", "/v1/tenant/school-teacher", "/school-teacher", "/api/school-teacher"})
public class SchoolTeacherController {

    private final SchoolTeacherRepository schoolTeacherRepository;
    private final SchoolRepository schoolRepository;
    private final TeacherRepository teacherRepository;

    public SchoolTeacherController(SchoolTeacherRepository schoolTeacherRepository, SchoolRepository schoolRepository, TeacherRepository teacherRepository) {
        this.schoolTeacherRepository = schoolTeacherRepository;
        this.schoolRepository = schoolRepository;
        this.teacherRepository = teacherRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TECADM', 'DIRETOR', 'COORDENADOR')")
    public ResponseEntity<Map<String, Object>> linkTeacherToSchool(@RequestBody Map<String, String> payload) {
        String schoolId = payload.get("school_id");
        String teacherId = payload.get("teacher_id");

        if (schoolId == null || teacherId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "school_id and teacher_id are required");
        }

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School not found"));

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));

        // Check if already linked
        boolean alreadyLinked = schoolTeacherRepository.findTeachersBySchoolId(schoolId)
                .stream().anyMatch(t -> t.getId().equals(teacherId));

        if (alreadyLinked) {
            return ResponseEntity.ok(Map.of("message", "Teacher already linked to school"));
        }

        SchoolTeacher schoolTeacher = new SchoolTeacher();
        schoolTeacher.setSchool(school);
        schoolTeacher.setTeacher(teacher);
        
        schoolTeacherRepository.save(schoolTeacher);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Teacher linked successfully"));
    }
}
