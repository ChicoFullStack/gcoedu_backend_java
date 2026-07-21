package com.gcoedu.core.domain.dto.tenant;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public final class DashboardDTO {

    private DashboardDTO() {
    }

    public record Quantity(long quantidade) {
    }

    public record RankingResponse<T>(
            List<T> ranking,
            long total,
            int limit,
            int offset
    ) {
    }

    public record StudentRankingItem(
            int position,
            @JsonProperty("student_id") String studentId,
            String name,
            String serie,
            @JsonProperty("class_name") String className,
            @JsonProperty("school_name") String schoolName,
            double media,
            @JsonProperty("completed_evaluations") int completedEvaluations
    ) {
    }

    public record ClassRankingItem(
            int posicao,
            @JsonProperty("class_id") String classId,
            String turma,
            String serie,
            double media,
            int acerto,
            @JsonProperty("acerto_percent") double acertoPercent,
            double conclusao,
            int alunos,
            int avaliacoes
    ) {
    }

    public record RecentEvaluationsResponse(List<RecentEvaluationItem> avaliacoes) {
    }

    public record RecentEvaluationItem(
            @JsonProperty("avaliacao_id") String avaliacaoId,
            String titulo,
            @JsonProperty("quantidade_alunos_fizeram") long quantidadeAlunosFizeram,
            @JsonProperty("quantidade_alunos_vao_fazer") long quantidadeAlunosVaoFazer,
            String prazo,
            double progresso,
            String status,
            String disciplina,
            String escola,
            List<String> escolas,
            Double media,
            @JsonProperty("data_inicio") String dataInicio
    ) {
    }

    public record QuestionsResponse(
            List<QuestionItem> questoes,
            long total,
            int limit,
            int offset
    ) {
    }

    public record QuestionItem(
            String id,
            String titulo,
            String disciplina,
            @JsonProperty("ano_serie") String anoSerie,
            String autor,
            @JsonProperty("data_criacao") String dataCriacao,
            String dificuldade,
            String classification,
            @JsonProperty("tipo_questao") String tipoQuestao,
            @JsonProperty("quantidade_respostas") long quantidadeRespostas,
            @JsonProperty("taxa_acerto") Double taxaAcerto,
            @JsonProperty("quantidade_avaliacoes") long quantidadeAvaliacoes,
            @JsonProperty("ultima_utilizacao") String ultimaUtilizacao,
            String habilidade
    ) {
    }

    public record SystemAnalysis(
            Metricas metricas,
            Conexao conexao,
            @JsonProperty("dados_tecnicos") DadosTecnicos dadosTecnicos,
            @JsonProperty("por_escopo") Map<String, Object> porEscopo,
            Map<String, Object> graficos,
            Administracao administracao
    ) {
    }

    public record Metricas(
            long students,
            long schools,
            long evaluations,
            long games,
            long users,
            long questions,
            long classes,
            long teachers,
            long certificates,
            @JsonProperty("last_sync") String lastSync
    ) {
    }

    public record Conexao(
            @JsonProperty("db_engine") String dbEngine,
            @JsonProperty("db_origem") String dbOrigem,
            @JsonProperty("db_status") String dbStatus,
            @JsonProperty("verificado_em") String verificadoEm,
            String ambiente,
            String timezone
    ) {
    }

    public record DadosTecnicos(
            String ambiente,
            String timestamp,
            String timezone
    ) {
    }

    public record Administracao(
            @JsonProperty("taxa_conclusao_geral") double taxaConclusaoGeral,
            @JsonProperty("total_sessoes") long totalSessoes,
            @JsonProperty("sessoes_concluidas") long sessoesConcluidas,
            @JsonProperty("certificados_emitidos") long certificadosEmitidos,
            @JsonProperty("alunos_com_pelo_menos_uma_avaliacao") long alunosComAvaliacao,
            @JsonProperty("percentual_participacao") double percentualParticipacao,
            @JsonProperty("escolas_ativas") long escolasAtivas,
            @JsonProperty("ultima_atividade") String ultimaAtividade,
            @JsonProperty("disciplinas_com_questoes") long disciplinasComQuestoes
    ) {
    }
}
