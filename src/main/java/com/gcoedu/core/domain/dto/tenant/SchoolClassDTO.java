package com.gcoedu.core.domain.dto.tenant;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record SchoolClassDTO(
    String id,
    @NotBlank(message = "O nome da turma é obrigatório") String name,
    String schoolId,
    UUID gradeId,
    String shift
) {}
