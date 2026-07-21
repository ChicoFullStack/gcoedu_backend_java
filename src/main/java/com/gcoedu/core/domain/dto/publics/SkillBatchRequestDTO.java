package com.gcoedu.core.domain.dto.publics;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SkillBatchRequestDTO {
    @NotEmpty(message = "Adicione pelo menos uma habilidade")
    @Size(max = 200, message = "Adicione no máximo 200 habilidades por vez")
    private List<@Valid SkillCreateDTO> skills;
}
