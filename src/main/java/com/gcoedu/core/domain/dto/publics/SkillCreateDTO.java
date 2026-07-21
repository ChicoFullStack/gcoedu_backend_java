package com.gcoedu.core.domain.dto.publics;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class SkillCreateDTO {
    @NotBlank(message = "O código da habilidade é obrigatório")
    @Size(max = 50, message = "O código da habilidade deve ter no máximo 50 caracteres")
    private String code;

    @NotBlank(message = "A descrição da habilidade é obrigatória")
    @Size(max = 4000, message = "A descrição da habilidade deve ter no máximo 4000 caracteres")
    private String description;

    @JsonProperty("subject_id")
    private String subjectId;

    @JsonProperty("grade_id")
    private UUID gradeId;

    @JsonProperty("grade_ids")
    private List<UUID> gradeIds;
}
