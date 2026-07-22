package com.gcoedu.core.controller;

import com.gcoedu.core.domain.dto.tenant.OmrUploadResponseDTO;
import com.gcoedu.core.service.JobProgressService;
import com.gcoedu.core.service.tenant.EvaluationOMRService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping(value = {"/api/v1/tenant/omr", "/v1/tenant/omr"})
public class OMRController {

    private final EvaluationOMRService evaluationOMRService;
    private final JobProgressService jobProgressService;

    public OMRController(EvaluationOMRService evaluationOMRService,
                         JobProgressService jobProgressService) {
        this.evaluationOMRService = evaluationOMRService;
        this.jobProgressService = jobProgressService;
    }

    /**
     * Recebe a foto do cartão, salva no MinIO e despacha OpenCV em background.
     * Retorna 202 Accepted + jobId para o frontend consultar o progresso.
     * Form-data: file (image/jpeg), gabaritoId, studentId
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TEACHER')")
    public ResponseEntity<OmrUploadResponseDTO> uploadAnswerSheet(
            @RequestParam("file") MultipartFile file,
            @RequestParam("gabaritoId") String gabaritoId,
            @RequestParam("studentId") String studentId) throws Exception {

        OmrUploadResponseDTO response = evaluationOMRService.receiveUpload(gabaritoId, studentId, file);
        return ResponseEntity.accepted().body(response);
    }

    /**
     * Consulta o progresso de um job OMR.
     * Retorna o hash Redis: status, percentage, error (se falhou).
     */
    @GetMapping("/progress/{jobId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TEACHER')")
    public ResponseEntity<Map<Object, Object>> getProgress(@PathVariable String jobId) {
        Map<Object, Object> status = jobProgressService.getJobStatus(jobId);
        if (status == null || status.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }
}
