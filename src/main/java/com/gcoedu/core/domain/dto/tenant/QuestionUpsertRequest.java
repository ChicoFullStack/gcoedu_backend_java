package com.gcoedu.core.domain.dto.tenant;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record QuestionUpsertRequest(
        @NotBlank(message = "O título é obrigatório")
        @Size(max = 255, message = "O título deve ter no máximo 255 caracteres")
        String title,
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
        String description,
        @JsonAlias("statement")
        @NotBlank(message = "O enunciado é obrigatório")
        @Size(max = 2000, message = "O enunciado deve ter no máximo 2000 caracteres")
        String text,
        @JsonProperty("formattedText")
        String formattedText,
        @JsonProperty("secondStatement")
        @Size(max = 2000, message = "O segundo enunciado deve ter no máximo 2000 caracteres")
        String secondStatement,
        @NotBlank(message = "O tipo da questão é obrigatório")
        @Pattern(
                regexp = "multipleChoice|dissertativa",
                message = "O tipo da questão deve ser multipleChoice ou dissertativa"
        )
        String type,
        @JsonProperty("subjectId")
        @NotBlank(message = "A disciplina é obrigatória")
        @Size(max = 36, message = "A disciplina informada é inválida")
        String subjectId,
        @JsonProperty("educationStageId")
        @Size(max = 36, message = "A etapa de ensino informada é inválida")
        String educationStageId,
        @NotBlank(message = "A série é obrigatória")
        @Size(max = 36, message = "A série informada é inválida")
        String grade,
        @NotBlank(message = "A dificuldade é obrigatória")
        @Pattern(
                regexp = "Abaixo do Básico|Básico|Adequado|Avançado",
                message = "A dificuldade informada é inválida"
        )
        String difficulty,
        @NotNull(message = "O valor da questão é obrigatório")
        @PositiveOrZero(message = "O valor da questão não pode ser negativo")
        Double value,
        @JsonAlias("correctOption")
        String solution,
        @JsonProperty("formattedSolution")
        String formattedSolution,
        @Size(max = 5, message = "Adicione no máximo cinco alternativas")
        List<@Valid QuestionOptionDTO> options,
        @Size(max = 10, message = "Selecione no máximo dez habilidades")
        List<@Size(max = 36, message = "Uma habilidade informada é inválida") String> skills,
        @Size(max = 20, message = "Adicione no máximo vinte tópicos")
        List<@Size(max = 100, message = "Cada tópico deve ter no máximo 100 caracteres") String> topics
) {}
