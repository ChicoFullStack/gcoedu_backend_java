package com.gcoedu.core.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping(value = {"/physical-tests", "/api/physical-tests"})
public class PhysicalTestsController {

    @PostMapping("/test/{testId}/process-correction")
    public ResponseEntity<Map<String, Object>> processCorrection(
            @PathVariable String testId,
            @RequestBody Map<String, Object> payload) {
        
        // Retorna um jobId para que o frontend consulte o progresso
        // Stub Fase 4
        return ResponseEntity.ok(Map.of(
                "job_id", "stub-job-" + System.currentTimeMillis(),
                "message", "Processamento em lote iniciado"
        ));
    }

    @GetMapping("/correction-progress/{jobId}")
    public ResponseEntity<Map<String, Object>> getCorrectionProgress(@PathVariable String jobId) {
        // Stub de progresso Fase 4
        return ResponseEntity.ok(Map.of(
                "status", "completed", // "pending", "processing", "completed", "failed"
                "progress", 100,
                "message", "Concluído"
        ));
    }

    @PostMapping("/upload-images")
    public ResponseEntity<Map<String, Object>> uploadImages(
            @RequestParam("file") MultipartFile[] files) {
        
        // Stub para upload múltiplo se o frontend enviar as imagens separadamente
        return ResponseEntity.ok(Map.of("success", true, "message", "Imagens recebidas"));
    }
}
