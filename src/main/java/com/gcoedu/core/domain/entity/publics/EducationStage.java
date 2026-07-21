package com.gcoedu.core.domain.entity.publics;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "education_stage", schema = "public")
@Data
@NoArgsConstructor
public class EducationStage {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;
}
