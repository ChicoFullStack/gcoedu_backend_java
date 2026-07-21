package com.gcoedu.core.domain.entity.tenant;

import com.gcoedu.core.domain.entity.publics.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "student_answers")
@Data
@NoArgsConstructor
public class StudentAnswer {

    @Id
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String answer;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt = LocalDateTime.now();

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "manual_score")
    private Double manualScore;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corrected_by")
    private User correctedBy;

    @Column(name = "corrected_at")
    private LocalDateTime correctedAt;
}
