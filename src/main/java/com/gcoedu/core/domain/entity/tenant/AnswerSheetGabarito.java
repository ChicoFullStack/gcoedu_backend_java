package com.gcoedu.core.domain.entity.tenant;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.UUID;

@Entity
@Table(name = "answer_sheet_gabaritos")
@Data
@NoArgsConstructor
public class AnswerSheetGabarito {
    @Id
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id")
    private Test test;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private SchoolClass schoolClass;

    @Column(name = "title")
    private String title;

    @Column(name = "num_questions", nullable = false)
    private Integer numQuestions;

    @Column(name = "use_blocks")
    private Boolean useBlocks = false;

    @Column(name = "separate_by_subject")
    private Boolean separateBySubject = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "blocks_config")
    private String blocksConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "correct_answers", nullable = false)
    private String correctAnswers;

    @Lob
    @Column(name = "template_block_1")
    private byte[] templateBlock1;
    
    @Lob
    @Column(name = "template_block_2")
    private byte[] templateBlock2;
}
