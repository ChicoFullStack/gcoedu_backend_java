package com.gcoedu.core.domain.entity.tenant;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

/**
 * Entidade associativa entre Test (Prova) e Question (Questão).
 * Guarda a posição da questão no cartão-resposta para
 * que o motor OMR saiba qual bolha mapear para qual questão.
 */
@Entity
@Table(name = "test_questions")
@Data
@NoArgsConstructor
public class TestQuestion {
    @Id
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    /** Posição/número da questão no cartão (1-based) */
    @Column(name = "\"order\"")
    private Integer orderIndex;

    /**
     * Peso ainda não persistido no schema compartilhado legado.
     * Mantido no contrato Java para compatibilidade da API.
     */
    @Transient
    private Double weight = 1.0;
}
