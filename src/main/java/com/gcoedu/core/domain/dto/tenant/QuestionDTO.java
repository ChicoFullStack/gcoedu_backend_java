package com.gcoedu.core.domain.dto.tenant;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public record QuestionDTO(
        String id,
        Integer number,
        String title,
        String description,
        String text,
        @JsonProperty("formattedText") String formattedText,
        @JsonProperty("secondStatement") String secondStatement,
        String type,
        String difficulty,
        Double value,
        String solution,
        @JsonProperty("correct_answer") String correctAnswer,
        @JsonProperty("formattedSolution") String formattedSolution,
        List<QuestionOptionDTO> options,
        List<QuestionOptionDTO> alternatives,
        List<String> skills,
        List<String> topics,
        Integer version,
        @JsonProperty("subjectId") String subjectId,
        QuestionReferenceDTO subject,
        QuestionReferenceDTO grade,
        @JsonProperty("educationStage") QuestionReferenceDTO educationStage,
        @JsonProperty("createdAt") LocalDateTime createdAt,
        @JsonProperty("updatedAt") LocalDateTime updatedAt,
        @JsonProperty("createdBy") QuestionReferenceDTO createdBy,
        @JsonProperty("lastModifiedBy") QuestionReferenceDTO lastModifiedBy,
        @JsonProperty("scopeType") String scopeType,
        QuestionPermissionsDTO permissions
) {}
