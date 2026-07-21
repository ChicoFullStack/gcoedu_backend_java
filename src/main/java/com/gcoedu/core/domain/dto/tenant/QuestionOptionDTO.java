package com.gcoedu.core.domain.dto.tenant;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

public record QuestionOptionDTO(
        @Size(max = 20, message = "O identificador da alternativa é inválido")
        String id,
        @Size(max = 500, message = "A alternativa deve ter no máximo 500 caracteres")
        String text,
        @JsonProperty("isCorrect") boolean isCorrect,
        Object image
) {}
