package com.gcoedu.core.controller;

import com.gcoedu.core.domain.dto.tenant.StudentDTO;
import com.gcoedu.core.service.tenant.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = {"/api/v1/tenant/students", "/v1/tenant/students", "/students", "/api/students"})
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TEACHER')")
    public ResponseEntity<List<StudentDTO>> getAllStudents() {
        return ResponseEntity.ok(studentService.findAll());
    }

    @GetMapping("/classes/{classId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TEACHER')")
    public ResponseEntity<List<StudentDTO>> getStudentsByClassId(@PathVariable String classId) {
        return ResponseEntity.ok(studentService.findBySchoolClassId(classId));
    }

    @GetMapping("/school/{schoolId}/class/{classId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TEACHER')")
    public ResponseEntity<List<StudentDTO>> getStudentsBySchoolAndClassId(@PathVariable String schoolId, @PathVariable String classId) {
        return ResponseEntity.ok(studentService.findBySchoolClassId(classId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TEACHER')")
    public ResponseEntity<StudentDTO> getStudentById(@PathVariable String id) {
        return ResponseEntity.ok(studentService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<StudentDTO> createStudent(@Valid @RequestBody StudentDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<StudentDTO> updateStudent(@PathVariable String id, @Valid @RequestBody StudentDTO dto) {
        return ResponseEntity.ok(studentService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> deleteStudent(@PathVariable String id) {
        studentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
