package com.gcoedu.core.controller;

import com.gcoedu.core.domain.dto.tenant.BaseDashboardResponseDTO;
import com.gcoedu.core.domain.dto.tenant.DashboardDTO;
import com.gcoedu.core.domain.dto.tenant.EvaluationStatsResponseDTO;
import com.gcoedu.core.service.tenant.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = {"", "/api"})
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard/comprehensive-stats")
    public ResponseEntity<BaseDashboardResponseDTO> getComprehensiveStats() {
        return ResponseEntity.ok(dashboardService.getComprehensiveStats());
    }

    @GetMapping("/evaluations/stats")
    public ResponseEntity<EvaluationStatsResponseDTO> getEvaluationStats() {
        return ResponseEntity.ok(dashboardService.getEvaluationStats());
    }

    @GetMapping("/dashboard/avisos/quantidade")
    public ResponseEntity<DashboardDTO.Quantity> getAvisosQuantidade() {
        return ResponseEntity.ok(new DashboardDTO.Quantity(0));
    }

    @GetMapping("/dashboard/ranking-alunos")
    public ResponseEntity<DashboardDTO.RankingResponse<DashboardDTO.StudentRankingItem>> getRankingAlunos(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        int safeLimit = clamp(limit, 1, 50);
        return ResponseEntity.ok(dashboardService.getRankingAlunos(safeLimit, Math.max(0, offset)));
    }

    @GetMapping("/dashboard/questoes")
    public ResponseEntity<DashboardDTO.QuestionsResponse> getQuestoes(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        int safeLimit = clamp(limit, 1, 50);
        return ResponseEntity.ok(dashboardService.getQuestoes(safeLimit, Math.max(0, offset)));
    }

    @GetMapping("/dashboard/avaliacoes-recentes")
    public ResponseEntity<DashboardDTO.RecentEvaluationsResponse> getAvaliacoesRecentes(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(dashboardService.getAvaliacoesRecentes(clamp(limit, 1, 50)));
    }

    @GetMapping("/dashboard/ranking-turmas")
    public ResponseEntity<DashboardDTO.RankingResponse<DashboardDTO.ClassRankingItem>> getRankingTurmas(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        int safeLimit = clamp(limit, 1, 100);
        return ResponseEntity.ok(dashboardService.getRankingTurmas(safeLimit, Math.max(0, offset)));
    }

    @GetMapping("/dashboard/analise-sistema")
    public ResponseEntity<DashboardDTO.SystemAnalysis> getSystemAnalysis() {
        return ResponseEntity.ok(dashboardService.getSystemAnalysis());
    }

    @GetMapping("/dashboard/admin")
    public ResponseEntity<BaseDashboardResponseDTO> getDashboardAdmin() {
        return ResponseEntity.ok(dashboardService.getComprehensiveStats());
    }

    private int clamp(int value, int minimum, int maximum) {
        return Math.min(maximum, Math.max(minimum, value));
    }
}
