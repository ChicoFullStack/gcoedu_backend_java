package com.gcoedu.core.domain.entity.tenant;

import com.gcoedu.core.domain.entity.publics.EducationStage;
import com.gcoedu.core.domain.entity.publics.Grade;
import com.gcoedu.core.domain.entity.publics.Subject;
import com.gcoedu.core.domain.entity.publics.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity(name = "TenantQuestion")
@Table(name = "question", schema = "public")
@Data
@NoArgsConstructor
public class Question {
    @Id
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(length = 2000, nullable = false)
    private String statement;

    @Column
    private Integer number;

    @Column(length = 2000)
    private String text;

    @Column(name = "formatted_text", columnDefinition = "TEXT")
    private String formattedText;

    @Column(name = "secondstatement", length = 2000)
    private String secondStatement;

    @Column
    private String title;

    @Column
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<Map<String, Object>> alternatives;

    @Column
    private String skill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_level")
    private Grade grade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "education_stage_id")
    private EducationStage educationStage;

    @Column(name = "difficulty_level")
    private String difficultyLevel;

    @Column(name = "correct_answer")
    private String correctAnswer;

    @Column(name = "formatted_solution", columnDefinition = "TEXT")
    private String formattedSolution;

    @Column(name = "question_type")
    private String questionType;

    @Column
    private Double value;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<String> topics;

    @Column
    private Integer version;

    @Column(name = "option_a", length = 500)
    private String optionA;

    @Column(name = "option_b", length = 500)
    private String optionB;

    @Column(name = "option_c", length = 500)
    private String optionC;

    @Column(name = "option_d", length = 500)
    private String optionD;

    @Column(name = "option_e", length = 500)
    private String optionE;

    /** Letra correta: A, B, C, D ou E */
    @Column(name = "correct_option", length = 1, nullable = false)
    private String correctOption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by")
    private User lastModifiedBy;

    @Column(name = "scope_type", columnDefinition = "question_scope_type")
    @ColumnTransformer(read = "scope_type::text", write = "?::question_scope_type")
    private String scopeType;

    @Column(name = "owner_city_id")
    private String ownerCityId;

    @Column(name = "owner_user_id")
    private String ownerUserId;

    @PreUpdate
    void updateTimestamp() {
        updatedAt = LocalDateTime.now();
    }
}
