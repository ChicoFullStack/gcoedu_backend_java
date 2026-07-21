package com.gcoedu.core.domain.dto.tenant;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record StudentDTO(
    String id,
    @NotBlank(message = "O nome do aluno é obrigatório") String name,
    String profilePicture,
    @NotBlank(message = "A matrícula é obrigatória") String registration,
    LocalDate birthDate,
    String userId,
    String gradeId,
    String classId,
    String schoolId
) {}
