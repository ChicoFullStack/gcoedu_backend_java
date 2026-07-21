package com.gcoedu.core.controller;

import com.gcoedu.core.domain.dto.publics.SubjectDTO;
import com.gcoedu.core.service.publics.SubjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = {"/subjects", "/api/subjects"})
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @GetMapping({"", "/"})
    public ResponseEntity<List<SubjectDTO>> getAllSubjects() {
        return ResponseEntity.ok(subjectService.findAll());
    }

    @PostMapping({"", "/"})
    public ResponseEntity<SubjectDTO> createSubject(@Valid @RequestBody SubjectDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subjectService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubjectDTO> updateSubject(
            @PathVariable String id,
            @Valid @RequestBody SubjectDTO dto
    ) {
        return ResponseEntity.ok(subjectService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubject(@PathVariable String id) {
        subjectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
