package com.gcoedu.core.mapper.publics;

import com.gcoedu.core.domain.dto.publics.GradeDTO;
import com.gcoedu.core.domain.entity.publics.Grade;
import org.springframework.stereotype.Component;

@Component
public class GradeMapper {

    public GradeDTO toDto(Grade entity) {
        if (entity == null) return null;
        GradeDTO dto = new GradeDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setSlug(entity.getSlug());
        dto.setFormat(entity.getFormat());
        return dto;
    }

    public Grade toEntity(GradeDTO dto) {
        if (dto == null) return null;
        Grade entity = new Grade();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setSlug(dto.getSlug());
        entity.setFormat(dto.getFormat());
        return entity;
    }
}
