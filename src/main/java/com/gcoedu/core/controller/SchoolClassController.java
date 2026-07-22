package com.gcoedu.core.controller;

import com.gcoedu.core.domain.dto.tenant.SchoolClassDTO;
import com.gcoedu.core.service.tenant.SchoolClassService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = {"/api/v1/tenant/classes", "/v1/tenant/classes", "/class", "/api/class", "/classes", "/api/classes"})
public class SchoolClassController {

    private final SchoolClassService classService;
    private final com.gcoedu.core.repository.tenant.TeacherClassRepository teacherClassRepository;

    public SchoolClassController(SchoolClassService classService, com.gcoedu.core.repository.tenant.TeacherClassRepository teacherClassRepository) {
        this.classService = classService;
        this.teacherClassRepository = teacherClassRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TECADM', 'TEACHER')")
    public ResponseEntity<List<SchoolClassDTO>> getAllClasses(@RequestParam(required = false) String schoolId) {
        if (schoolId != null) {
            return ResponseEntity.ok(classService.findBySchoolId(schoolId));
        }
        return ResponseEntity.ok(classService.findAll());
    }

    @GetMapping("/school/{schoolId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TECADM', 'TEACHER')")
    public ResponseEntity<List<SchoolClassDTO>> getClassesBySchoolId(@PathVariable String schoolId) {
        return ResponseEntity.ok(classService.findBySchoolId(schoolId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TECADM', 'TEACHER')")
    public ResponseEntity<SchoolClassDTO> getClassById(@PathVariable String id) {
        return ResponseEntity.ok(classService.findById(id));
    }

    @GetMapping("/{id}/teachers")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TECADM', 'TEACHER', 'DIRETOR', 'COORDENADOR')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<java.util.Map<String, Object>>> getClassTeachers(@PathVariable String id) {
        List<com.gcoedu.core.domain.entity.tenant.Teacher> teachers = teacherClassRepository.findTeachersByClassId(id);
        
        List<java.util.Map<String, Object>> result = teachers.stream().map(t -> {
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", t.getId());
            map.put("name", t.getName());
            map.put("registration", t.getRegistration());
            if (t.getUser() != null) {
                map.put("email", t.getUser().getEmail());
                map.put("role", t.getUser().getRole() != null ? t.getUser().getRole().name().toLowerCase() : "professor");
            } else {
                map.put("email", "");
                map.put("role", "professor");
            }
            return map;
        }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<SchoolClassDTO> createClass(@Valid @RequestBody SchoolClassDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(classService.create(dto));
    }

    @PostMapping("/{classId}/add_student")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> addStudentsToClass(@PathVariable String classId, @RequestBody java.util.Map<String, List<String>> body) {
        List<String> studentIds = body.get("student_ids");
        if (studentIds != null && !studentIds.isEmpty()) {
            classService.addStudents(classId, studentIds);
        }
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<SchoolClassDTO> updateClass(@PathVariable String id, @Valid @RequestBody SchoolClassDTO dto) {
        return ResponseEntity.ok(classService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> deleteClass(@PathVariable String id) {
        classService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
