package com.gcoedu.core.domain.entity.tenant;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.UUID;

@Entity
@Table(name = "answer_sheet_results")
@Data
@NoArgsConstructor
public class AnswerSheetResult {
    @Id
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gabarito_id", nullable = false)
    private AnswerSheetGabarito gabarito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detected_answers", nullable = false)
    private String detectedAnswers;

    @Column(name = "correct_answers", nullable = false)
    private Integer correctAnswersCount;

    @Column(name = "score_percentage", nullable = false)
    private Double scorePercentage;

    @Column(nullable = false)
    private Double grade;
}
