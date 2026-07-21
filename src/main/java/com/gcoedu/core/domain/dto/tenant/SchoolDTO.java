package com.gcoedu.core.domain.dto.tenant;

import jakarta.validation.constraints.NotBlank;

public record SchoolDTO(
    String id,
    @NotBlank(message = "O nome da escola é obrigatório") String name,
    String address,
    String domain,
    String cityId
) {}
