package com.gcoedu.core.controller;

import com.gcoedu.core.domain.entity.tenant.EvaluationResult;
import com.gcoedu.core.repository.tenant.EvaluationResultRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = {"/evaluation-results", "/api/evaluation-results"})
public class EvaluationResultsController {

    private final EvaluationResultRepository evaluationResultRepository;

    public EvaluationResultsController(EvaluationResultRepository evaluationResultRepository) {
        this.evaluationResultRepository = evaluationResultRepository;
    }

    @GetMapping({"/alunos", "/alunos/"})
    public ResponseEntity<List<EvaluationResult>> getResultadosAlunos(@RequestParam(name = "avaliacao_id", required = false) String avaliacaoId) {
        if (avaliacaoId != null) {
            return ResponseEntity.ok(evaluationResultRepository.findByTestId(avaliacaoId));
        }
        return ResponseEntity.ok(evaluationResultRepository.findAll());
    }

    @GetMapping("/opcoes-filtros")
    public ResponseEntity<java.util.Map<String, Object>> getOpcoesFiltros() {
        return ResponseEntity.ok(java.util.Map.of(
            "estados", java.util.Collections.emptyList(),
            "municipios", java.util.Collections.emptyList(),
            "escolas", java.util.Collections.emptyList(),
            "series", java.util.Collections.emptyList(),
            "turmas", java.util.Collections.emptyList(),
            "avaliacoes", java.util.Collections.emptyList()
        ));
    }
}
