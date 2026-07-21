package com.gcoedu.core.domain.dto.tenant;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

public record TestDTO(
    String id,
    @NotBlank(message = "O título da prova é obrigatório") String title,
    String description,
    String status,
    
    @JsonProperty("created_by")
    String creatorId,
    
    @JsonProperty("subject")
    String subjectId,
    
    String type,
    String model,
    String course,
    String grade,
    
    @JsonProperty("time_limit")
    String timeLimit,
    
    @JsonProperty("end_time")
    String endTime,
    
    Integer duration,
    
    @JsonProperty("evaluation_mode")
    String evaluationMode,
    
    List<String> classes,
    List<Map<String, Object>> questions
) {}
