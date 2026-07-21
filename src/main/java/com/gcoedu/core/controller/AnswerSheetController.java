package com.gcoedu.core.controller;

import com.gcoedu.core.domain.dto.tenant.report.NovaRespostaAPI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = {"/answer-sheets", "/api/answer-sheets"})
public class AnswerSheetController {

    @GetMapping("/resultados-agregados")
    public ResponseEntity<NovaRespostaAPI> getResultadosAgregados(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String municipio,
            @RequestParam(required = false) String escola,
            @RequestParam(required = false) String avaliacao) {
        
        // Retorna stub básico usando as classes criadas no DTO
        // TODO: Mapear logicamente com base nos dados do banco (Fase 1: Skeleton)
        NovaRespostaAPI response = NovaRespostaAPI.builder()
                .nivel_granularidade(avaliacao != null ? "avaliacao" : (escola != null ? "escola" : "municipio"))
                .build();
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/correct-new")
    public ResponseEntity<java.util.Map<String, Object>> correctNew(@RequestBody java.util.Map<String, Object> payload) {
        // Inicia a correção individual (stub da Fase 4)
        return ResponseEntity.ok(java.util.Map.of("job_id", "job-correct-" + System.currentTimeMillis()));
    }

    @PostMapping("/process-correction")
    public ResponseEntity<java.util.Map<String, Object>> processCorrection(@RequestBody java.util.Map<String, Object> payload) {
        // Processa as marcações alteradas manualmente (stub)
        return ResponseEntity.ok(java.util.Map.of("success", true, "message", "Correção salva"));
    }

    @GetMapping("/correction-progress/{jobId}")
    public ResponseEntity<java.util.Map<String, Object>> getCorrectionProgress(@PathVariable String jobId) {
        return ResponseEntity.ok(java.util.Map.of("status", "completed", "progress", 100));
    }

    @GetMapping("/manual-entry")
    public ResponseEntity<java.util.Map<String, Object>> getManualEntry(
            @RequestParam(required = false) String gabarito_id,
            @RequestParam(required = false) String test_id,
            @RequestParam String student_id) {
        // Stub para o formulário de correção manual
        return ResponseEntity.ok(java.util.Map.of(
            "student_id", student_id,
            "answers", java.util.Collections.emptyMap()
        ));
    }
}
