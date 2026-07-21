package com.gcoedu.core.domain.dto.publics;

import java.util.List;

public record SkillBatchResponseDTO(
        List<SkillDTO> created,
        int count,
        List<SkillBatchErrorDTO> errors
) {
}
