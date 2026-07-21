package com.gcoedu.core.controller;

import com.gcoedu.core.domain.dto.tenant.QuestionBulkDeleteRequest;
import com.gcoedu.core.domain.dto.tenant.QuestionBulkDeleteResponse;
import com.gcoedu.core.domain.dto.tenant.QuestionDTO;
import com.gcoedu.core.domain.dto.tenant.QuestionUpsertRequest;
import com.gcoedu.core.service.tenant.QuestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = {"/api/v1/tenant/questions", "/questions", "/api/questions"})
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping({"", "/"})
    public ResponseEntity<List<QuestionDTO>> getAllQuestions(
            @RequestParam(name = "created_by", required = false) String createdBy,
            @RequestParam(name = "subject_id", required = false) String subjectId,
            @RequestParam(name = "subjectId", required = false) String subjectIdAlias,
            @RequestParam(required = false) String type
    ) {
        return ResponseEntity.ok(questionService.findAll(
                createdBy,
                subjectId != null ? subjectId : subjectIdAlias,
                type
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuestionDTO> getQuestionById(@PathVariable String id) {
        return ResponseEntity.ok(questionService.findById(id));
    }

    @PostMapping({"", "/"})
    public ResponseEntity<QuestionDTO> createQuestion(
            @Valid @RequestBody QuestionUpsertRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(questionService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuestionDTO> updateQuestion(
            @PathVariable String id,
            @Valid @RequestBody QuestionUpsertRequest request
    ) {
        return ResponseEntity.ok(questionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable String id) {
        questionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping({"", "/"})
    public ResponseEntity<QuestionBulkDeleteResponse> deleteQuestions(
            @Valid @RequestBody QuestionBulkDeleteRequest request
    ) {
        return ResponseEntity.ok(questionService.deleteBulk(request.ids()));
    }
}
