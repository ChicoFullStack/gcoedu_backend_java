package com.gcoedu.core.domain.dto.tenant;

import java.util.List;

public record AnswerSheetResultDTO(
    String id,
    String gabaritoId,
    String studentId,
    String studentName,
    List<String> detectedAnswers,
    Integer correctAnswersCount,
    Double scorePercentage,
    Double grade
) {}
