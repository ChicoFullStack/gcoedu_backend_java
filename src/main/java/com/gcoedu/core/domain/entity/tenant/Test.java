package com.gcoedu.core.domain.entity.tenant;

import com.gcoedu.core.domain.entity.publics.Subject;
import com.gcoedu.core.domain.entity.publics.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "test")
@Data
@NoArgsConstructor
public class Test {
    @Id
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(length = 20)
    private String status = "pendente";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject")
    private Subject subjectRel;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Novos campos para suportar o modelo "Olimpiadas" / Avaliações do frontend
    @Column(length = 20)
    private String type; // OLIMPIADA, PROVA

    @Column(length = 50)
    private String model;

    @Column(length = 50)
    private String course;

    @Column(name = "grade_id")
    private UUID gradeId;

    @Column(name = "time_limit")
    private LocalDateTime timeLimit;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column
    private Integer duration;

    @Column(length = 50, name = "evaluation_mode")
    private String evaluationMode;
}
