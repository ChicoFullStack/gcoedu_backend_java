package com.gcoedu.core.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = {"/teacher/directors", "/api/teacher/directors"})
public class TeacherDirectorsController {

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<Object>> getDirectors() {
        // Return empty list for now, to satisfy frontend until Directors logic is fully implemented
        return ResponseEntity.ok(java.util.Collections.emptyList());
    }
}
