package com.gcoedu.core.domain.dto.tenant.report;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class NovaRespostaAPI {
    private String nivel_granularidade; // 'municipio' | 'escola' | 'serie' | 'turma' | 'avaliacao'
    private Object filtros_aplicados;
    private EstatisticasGerais estatisticas_gerais;
    private List<Object> resultados_por_disciplina;
    private ResultadosDetalhados resultados_detalhados;
    private TabelaDetalhada tabela_detalhada;
    private List<RankingItem> ranking;
    private OpcoesProximosFiltros opcoes_proximos_filtros;

    @Data
    @Builder
    public static class EstatisticasGerais {
        private String tipo;
        private String nome;
        private String estado;
        private String municipio;
        private String escola;
        private String serie;
    }

    @Data
    @Builder
    public static class ResultadosDetalhados {
        private List<EvaluationResult> avaliacoes;
        private Object paginacao;
    }

    @Data
    @Builder
    public static class EvaluationResult {
        private String id;
        private String titulo;
        private String disciplina;
        private String curso;
        private String serie;
        private String turma;
        private String escola;
        private String municipio;
        private String estado;
        private String data_aplicacao;
        private String status; // 'finalized' | 'in_progress' | 'pending' | 'concluida' | 'em_andamento' | 'pendente'
        private int total_alunos;
        private int alunos_participantes;
        private int alunos_pendentes;
        private int alunos_ausentes;
        private double media_nota;
        private double media_proficiencia;
        private Object distribuicao_classificacao;
    }

    @Data
    @Builder
    public static class TabelaDetalhada {
        private List<Disciplina> disciplinas;

        @Data
        @Builder
        public static class Disciplina {
            private String id;
            private String nome;
            private List<Questao> questoes;
            private List<Aluno> alunos;

            @Data
            @Builder
            public static class Questao {
                private int numero;
                private String habilidade;
                private String codigo_habilidade;
                private String question_id;
            }

            @Data
            @Builder
            public static class Aluno {
                private String id;
                private String nome;
                private String escola;
                private String serie;
                private String turma;
                private List<RespostaQuestao> respostas_por_questao;
                private int total_acertos;
                private int total_erros;
                private int total_respondidas;
                private int total_questoes_disciplina;
                private String nivel_proficiencia;
                private double nota;
                private double proficiencia;

                @Data
                @Builder
                public static class RespostaQuestao {
                    private int questao;
                    private boolean acertou;
                    private boolean respondeu;
                    private String resposta;
                }
            }
        }
    }

    @Data
    @Builder
    public static class RankingItem {
        private int posicao;
        private String descricao;
        private RankingAluno aluno;

        @Data
        @Builder
        public static class RankingAluno {
            private String id;
            private String nome;
            private String escola;
            private String serie;
            private String turma;
            private int total_acertos;
            private int total_respondidas;
            private double nota;
            private double proficiencia;
            private String nivel_proficiencia;
        }
    }

    @Data
    @Builder
    public static class OpcoesProximosFiltros {
        private List<FiltroOption> avaliacoes;
        private List<FiltroOption> escolas;
        private List<FiltroOption> series;
        private List<FiltroOption> turmas;
        private Boolean maximo_alcancado;

        @Data
        @Builder
        public static class FiltroOption {
            private String id;
            private String name;
            private String titulo; // para avaliacoes
        }
    }
}
