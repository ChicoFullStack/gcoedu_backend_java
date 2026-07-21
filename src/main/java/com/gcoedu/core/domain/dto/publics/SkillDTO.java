package com.gcoedu.core.domain.dto.publics;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class SkillDTO {
    private UUID id;
    private String code;
    private String description;

    @JsonProperty("subject_id")
    private String subjectId;

    @JsonProperty("grade_ids")
    private List<UUID> gradeIds;

    @JsonProperty("grade_id")
    public UUID getGradeId() {
        return gradeIds == null || gradeIds.isEmpty() ? null : gradeIds.getFirst();
    }
}
