package com.gcoedu.core.controller;

import com.gcoedu.core.domain.dto.publics.SkillBatchRequestDTO;
import com.gcoedu.core.domain.dto.publics.SkillBatchResponseDTO;
import com.gcoedu.core.domain.dto.publics.SkillCreateDTO;
import com.gcoedu.core.domain.dto.publics.SkillDTO;
import com.gcoedu.core.service.publics.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = {"/skills", "/api/skills"})
@RequiredArgsConstructor
public class SkillsController {

    private static final String QUESTION_AUTHOR_ROLES =
            "hasAnyRole('ADMIN', 'TECADM', 'DIRETOR', 'COORDENADOR', 'PROFESSOR')";

    private final SkillService skillService;

    @GetMapping({"", "/"})
    @PreAuthorize(QUESTION_AUTHOR_ROLES)
    public ResponseEntity<List<SkillDTO>> getSkills(
            @RequestParam(name = "subject_id", required = false) String subjectId,
            @RequestParam(name = "grade_id", required = false) UUID gradeId
    ) {
        return ResponseEntity.ok(skillService.findAll(subjectId, gradeId));
    }

    @GetMapping("/grade/{gradeId}")
    @PreAuthorize(QUESTION_AUTHOR_ROLES)
    public ResponseEntity<List<SkillDTO>> getSkillsByGrade(@PathVariable UUID gradeId) {
        return ResponseEntity.ok(skillService.findAll(null, gradeId));
    }

    @GetMapping("/subject/{subjectId}")
    @PreAuthorize(QUESTION_AUTHOR_ROLES)
    public ResponseEntity<List<SkillDTO>> getSkillsBySubject(@PathVariable String subjectId) {
        return ResponseEntity.ok(skillService.findAll(subjectId, null));
    }

    @PostMapping({"", "/"})
    @PreAuthorize(QUESTION_AUTHOR_ROLES)
    public ResponseEntity<SkillDTO> createSkill(@Valid @RequestBody SkillCreateDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(skillService.create(request));
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECADM')")
    public ResponseEntity<SkillBatchResponseDTO> createSkillsBatch(
            @Valid @RequestBody SkillBatchRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(skillService.createBatch(request));
    }

    @DeleteMapping("/{skillId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSkill(@PathVariable UUID skillId) {
        skillService.delete(skillId);
        return ResponseEntity.noContent().build();
    }
}
