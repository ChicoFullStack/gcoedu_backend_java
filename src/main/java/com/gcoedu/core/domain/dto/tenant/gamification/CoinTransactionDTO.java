package com.gcoedu.core.domain.dto.tenant.gamification;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CoinTransactionDTO {
    private String id;
    private String student_id;
    private int amount;
    private String reason;
    private String description;
    private int balance_after;
    private LocalDateTime created_at;
}
