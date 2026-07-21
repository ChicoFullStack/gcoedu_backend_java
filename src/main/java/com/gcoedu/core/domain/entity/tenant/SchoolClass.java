package com.gcoedu.core.domain.entity.tenant;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "class")
@Data
@NoArgsConstructor
public class SchoolClass {
    @Id
    @Column(length = 36)
    private String id = java.util.UUID.randomUUID().toString();

    @Column(length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;

    @Column(name = "grade_id")
    private UUID gradeId;

    @Column(length = 50)
    private String shift;
}
