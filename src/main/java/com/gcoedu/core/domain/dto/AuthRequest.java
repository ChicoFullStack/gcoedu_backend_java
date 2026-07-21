package com.gcoedu.core.domain.dto;

import lombok.Data;

@Data
public class AuthRequest {
    private String identifier;
    private String registration;
    private String password;

    public String getResolvedIdentifier() {
        if (identifier != null && !identifier.isEmpty()) return identifier;
        return registration;
    }
}
