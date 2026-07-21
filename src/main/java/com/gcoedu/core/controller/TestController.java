package com.gcoedu.core.controller;

import com.gcoedu.core.domain.dto.tenant.TestDTO;
import com.gcoedu.core.domain.entity.tenant.TestQuestion;
import com.gcoedu.core.service.tenant.TestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Collections;

@RestController
@RequestMapping(value = {"/api/v1/tenant/tests", "/test", "/api/test"})
public class TestController {

    private final TestService testService;

    public TestController(TestService testService) {
        this.testService = testService;
    }

    @GetMapping(value = {"", "/"})
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TEACHER')")
    public ResponseEntity<Object> getAllTests(
            @RequestParam(required = false) Integer per_page,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String order) {
        
        if (per_page != null) {
            // Frontend fallback pagination expected format
            return ResponseEntity.ok(Map.of(
                "data", testService.findAll(),
                "content", testService.findAll(),
                "totalElements", 0,
                "totalPages", 0,
                "pageNumber", 0
            ));
        }
        return ResponseEntity.ok(testService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TEACHER')")
    public ResponseEntity<TestDTO> getTestById(@PathVariable String id) {
        return ResponseEntity.ok(testService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TEACHER')")
    public ResponseEntity<TestDTO> createTest(@Valid @RequestBody TestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(testService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TEACHER')")
    public ResponseEntity<TestDTO> updateTest(@PathVariable String id, @Valid @RequestBody TestDTO dto) {
        return ResponseEntity.ok(testService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> deleteTest(@PathVariable String id) {
        testService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // --- Gerenciamento de Questões da Prova ---

    @GetMapping("/{id}/questions")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TEACHER')")
    public ResponseEntity<List<TestQuestion>> getQuestions(@PathVariable String id) {
        return ResponseEntity.ok(testService.getQuestionsForTest(id));
    }

    @PostMapping("/{testId}/questions/{questionId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TEACHER')")
    public ResponseEntity<Void> addQuestion(
            @PathVariable String testId,
            @PathVariable String questionId,
            @RequestParam(defaultValue = "0") int orderIndex,
            @RequestParam(defaultValue = "1.0") double weight) {
        testService.addQuestion(testId, questionId, orderIndex, weight);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{testId}/questions/{questionId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TEACHER')")
    public ResponseEntity<Void> removeQuestion(@PathVariable String testId, @PathVariable String questionId) {
        testService.removeQuestion(testId, questionId);
        return ResponseEntity.noContent().build();
    }

    // --- STUBS: Integração com Frontend (Olimpíadas / Avaliações) ---

    @GetMapping("/{id}/classes")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TEACHER')")
    public ResponseEntity<List<Object>> getClasses(@PathVariable String id) {
        return ResponseEntity.ok(Collections.emptyList());
    }

    @PostMapping("/{id}/classes")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> addClasses(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/classes/remove")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> removeClassesBody(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{testId}/classes/{classId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> removeClass(@PathVariable String testId, @PathVariable String classId) {
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/apply")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> applyToClasses(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/apply-olympics")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> applyToStudents(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{testId}/olympics/{studentId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> removeStudentApplication(@PathVariable String testId, @PathVariable String studentId) {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/applied-students")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, Object>> getAppliedStudents(@PathVariable String id) {
        return ResponseEntity.ok(Map.of("students", Collections.emptyList()));
    }

    @GetMapping("/my-class/tests")
    @PreAuthorize("hasAnyRole('TEACHER')")
    public ResponseEntity<List<Object>> getMyClassTests() {
        return ResponseEntity.ok(Collections.emptyList());
    }
}
