package com.gcoedu.core.mapper.tenant;

import com.gcoedu.core.domain.dto.tenant.SchoolDTO;
import com.gcoedu.core.domain.entity.tenant.School;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SchoolMapper {
    
    @Mapping(target = "cityId", source = "city.id")
    SchoolDTO toDto(School school);

    @Mapping(target = "city", ignore = true)
    School toEntity(SchoolDTO dto);

    @Mapping(target = "city", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(SchoolDTO dto, @MappingTarget School entity);
}
