package com.gcoedu.core.mapper.tenant;

import com.gcoedu.core.domain.dto.tenant.SchoolClassDTO;
import com.gcoedu.core.domain.entity.tenant.SchoolClass;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SchoolClassMapper {
    @Mapping(target = "schoolId", source = "school.id")
    SchoolClassDTO toDto(SchoolClass schoolClass);

    @Mapping(target = "school", ignore = true)
    SchoolClass toEntity(SchoolClassDTO dto);

    @Mapping(target = "school", ignore = true)
    @Mapping(target = "id", ignore = true)
    void updateEntity(SchoolClassDTO dto, @MappingTarget SchoolClass entity);
}
