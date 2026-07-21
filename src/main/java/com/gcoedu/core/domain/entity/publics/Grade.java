package com.gcoedu.core.domain.entity.publics;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "grade", schema = "public")
@Data
@NoArgsConstructor
public class Grade {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(length = 100)
    private String name;

    @Column(length = 50)
    private String slug;

    @Column(length = 20)
    private String format;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "education_stage_id", nullable = false)
    private EducationStage educationStage;
}
