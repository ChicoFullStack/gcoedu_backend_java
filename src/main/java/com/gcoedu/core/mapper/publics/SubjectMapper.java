package com.gcoedu.core.mapper.publics;

import com.gcoedu.core.domain.dto.publics.SubjectDTO;
import com.gcoedu.core.domain.entity.publics.Subject;
import org.springframework.stereotype.Component;

@Component
public class SubjectMapper {

    public SubjectDTO toDto(Subject entity) {
        if (entity == null) return null;
        SubjectDTO dto = new SubjectDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        return dto;
    }

    public Subject toEntity(SubjectDTO dto) {
        if (dto == null) return null;
        Subject entity = new Subject();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        return entity;
    }
}
