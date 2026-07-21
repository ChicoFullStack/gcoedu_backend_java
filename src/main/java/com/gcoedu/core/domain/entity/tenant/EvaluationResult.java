package com.gcoedu.core.domain.entity.tenant;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "evaluation_results")
@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EvaluationResult {

    @Id
    @EqualsAndHashCode.Include
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "test_id", nullable = false, length = 36)
    private String testId;

    @Column(name = "student_id", nullable = false, length = 36)
    private String studentId;

    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @Column(name = "correct_answers", nullable = false)
    private Integer correctAnswers;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    @Column(name = "score_percentage", nullable = false)
    private Double scorePercentage;

    @Column(nullable = false)
    private Double grade;

    @Column(nullable = false)
    private Double proficiency;

    @Column(nullable = false, length = 50)
    private String classification;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "subject_results", columnDefinition = "jsonb")
    private Map<String, Object> subjectResults;

    @Column(name = "school_id_snapshot", length = 36)
    private String schoolIdSnapshot;

    @Column(name = "class_id_snapshot", length = 36)
    private String classIdSnapshot;

    @Column(name = "grade_id_snapshot", length = 36)
    private String gradeIdSnapshot;

    @Column(name = "enrollment_id_snapshot", length = 36)
    private String enrollmentIdSnapshot;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt = LocalDateTime.now();
}
