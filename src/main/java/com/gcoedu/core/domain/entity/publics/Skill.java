package com.gcoedu.core.domain.entity.publics;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "skills", schema = "public")
@Data
@NoArgsConstructor
public class Skill {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(columnDefinition = "TEXT", nullable = false)
    private String code;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "skill_grade",
        schema = "public",
        joinColumns = @JoinColumn(name = "skill_id"),
        inverseJoinColumns = @JoinColumn(name = "grade_id")
    )
    private List<Grade> grades;
}
