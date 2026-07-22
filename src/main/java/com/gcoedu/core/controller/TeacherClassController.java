package com.gcoedu.core.controller;

import com.gcoedu.core.domain.entity.tenant.SchoolClass;
import com.gcoedu.core.domain.entity.tenant.Teacher;
import com.gcoedu.core.domain.entity.tenant.TeacherClass;
import com.gcoedu.core.repository.tenant.TeacherClassRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = {"/api/v1/tenant/teacher-class", "/v1/tenant/teacher-class", "/teacher-class", "/api/teacher-class"})
public class TeacherClassController {

    private final TeacherClassRepository teacherClassRepository;

    public TeacherClassController(TeacherClassRepository teacherClassRepository) {
        this.teacherClassRepository = teacherClassRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TECADM', 'DIRETOR', 'COORDENADOR')")
    public ResponseEntity<Object> createTeacherClass(@RequestBody Map<String, Object> payload) {
        String teacherId = (String) payload.get("teacher_id");
        String classId = (String) payload.get("class_id");

        if (teacherId == null || classId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "teacher_id and class_id are required"));
        }

        Teacher teacher = new Teacher();
        teacher.setId(teacherId);
        
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(classId);

        TeacherClass teacherClass = new TeacherClass();
        teacherClass.setId(UUID.randomUUID().toString());
        teacherClass.setTeacher(teacher);
        teacherClass.setSchoolClass(schoolClass);

        teacherClassRepository.save(teacherClass);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Professor vinculado à turma com sucesso");
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
