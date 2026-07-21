package com.gcoedu.core.controller;

import com.gcoedu.core.domain.dto.publics.GradeDTO;
import com.gcoedu.core.service.publics.GradeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = {"/grades", "/api/grades"})
public class GradeController {

    private final GradeService gradeService;

    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @GetMapping({"", "/"})
    public ResponseEntity<List<GradeDTO>> getAllGrades() {
        return ResponseEntity.ok(gradeService.findAll());
    }

    @GetMapping({"/education-stage/{stageId}", "/by-education-stage/{stageId}"})
    public ResponseEntity<List<GradeDTO>> getGradesByEducationStageId(@org.springframework.web.bind.annotation.PathVariable java.util.UUID stageId) {
        return ResponseEntity.ok(gradeService.findByEducationStageId(stageId));
    }
}
