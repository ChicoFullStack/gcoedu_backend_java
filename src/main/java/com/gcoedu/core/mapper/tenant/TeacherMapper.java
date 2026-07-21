package com.gcoedu.core.mapper.tenant;

import com.gcoedu.core.domain.dto.tenant.TeacherDTO;
import com.gcoedu.core.domain.entity.tenant.Teacher;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TeacherMapper {
    @Mapping(target = "userId", source = "user.id")
    TeacherDTO toDto(Teacher teacher);

    @Mapping(target = "user", ignore = true)
    Teacher toEntity(TeacherDTO dto);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(TeacherDTO dto, @MappingTarget Teacher entity);
}
