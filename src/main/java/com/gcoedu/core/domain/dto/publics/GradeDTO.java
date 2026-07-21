package com.gcoedu.core.domain.dto.publics;

import lombok.Data;
import java.util.UUID;

@Data
public class GradeDTO {
    private UUID id;
    private String name;
    private String slug;
    private String format;
}
