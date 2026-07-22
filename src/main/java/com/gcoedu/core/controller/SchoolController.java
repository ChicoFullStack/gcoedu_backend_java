package com.gcoedu.core.controller;

import com.gcoedu.core.service.tenant.SchoolService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.gcoedu.core.domain.dto.tenant.SchoolDTO;
import java.util.List;

@RestController
@RequestMapping(value = {"/api/v1/tenant/schools", "/v1/tenant/schools", "/school", "/api/school", "/schools", "/api/schools"})
public class SchoolController {

    private final SchoolService schoolService;

    public SchoolController(SchoolService schoolService) {
        this.schoolService = schoolService;
    }

    @GetMapping({"", "/"})
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TEACHER')")
    public ResponseEntity<List<SchoolDTO>> getAllSchools() {
        return ResponseEntity.ok(schoolService.findAll());
    }

    @GetMapping("/city/{cityId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TEACHER')")
    public ResponseEntity<List<SchoolDTO>> getSchoolsByCityId(@PathVariable String cityId) {
        return ResponseEntity.ok(schoolService.findByCityId(cityId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TEACHER')")
    public ResponseEntity<SchoolDTO> getSchoolById(@PathVariable String id) {
        return ResponseEntity.ok(schoolService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<SchoolDTO> createSchool(@Valid @RequestBody SchoolDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schoolService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<SchoolDTO> updateSchool(@PathVariable String id, @Valid @RequestBody SchoolDTO dto) {
        return ResponseEntity.ok(schoolService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> deleteSchool(@PathVariable String id) {
        schoolService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
