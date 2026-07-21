package com.gcoedu.core.domain.dto.tenant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EvaluationStatsResponseDTO {
    private long total;
    private long this_month;
    private long total_questions;
    private double average_questions;
}
