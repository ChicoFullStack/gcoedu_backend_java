package com.gcoedu.core.domain.dto.tenant;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record TeacherDTO(
    String id,
    @NotBlank(message = "O nome é obrigatório") String name,
    String profilePicture,
    @NotBlank(message = "O registro é obrigatório") String registration,
    LocalDate birthDate,
    String userId
) {}
