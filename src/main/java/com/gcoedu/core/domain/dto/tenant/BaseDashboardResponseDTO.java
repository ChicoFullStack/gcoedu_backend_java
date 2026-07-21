package com.gcoedu.core.domain.dto.tenant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BaseDashboardResponseDTO {
    private long students;
    private long schools;
    private long evaluations;
    private long games;
    private long users;
    private long questions;
    private long classes;
    private long teachers;
    private String last_sync;
}
