package com.gcoedu.core.domain.dto.tenant;

import com.fasterxml.jackson.annotation.JsonProperty;

public record QuestionPermissionsDTO(
        @JsonProperty("canEdit") boolean canEdit,
        @JsonProperty("canDelete") boolean canDelete
) {}
