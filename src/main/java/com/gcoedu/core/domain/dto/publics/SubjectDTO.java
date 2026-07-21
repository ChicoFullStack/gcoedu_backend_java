package com.gcoedu.core.domain.dto.publics;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubjectDTO {
    private String id;

    @NotBlank(message = "O nome da disciplina é obrigatório")
    @Size(max = 100, message = "O nome da disciplina deve ter no máximo 100 caracteres")
    private String name;
}
