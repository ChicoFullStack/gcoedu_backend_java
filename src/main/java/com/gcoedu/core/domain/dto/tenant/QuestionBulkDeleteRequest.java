package com.gcoedu.core.domain.dto.tenant;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record QuestionBulkDeleteRequest(
        @NotEmpty(message = "Selecione ao menos uma questão")
        List<String> ids
) {}
