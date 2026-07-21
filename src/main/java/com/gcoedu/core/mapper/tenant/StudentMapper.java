package com.gcoedu.core.mapper.tenant;

import com.gcoedu.core.domain.dto.tenant.StudentDTO;
import com.gcoedu.core.domain.entity.tenant.Student;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface StudentMapper {
    
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "classId", source = "schoolClass.id")
    @Mapping(target = "schoolId", source = "school.id")
    StudentDTO toDto(Student student);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "schoolClass", ignore = true)
    @Mapping(target = "school", ignore = true)
    Student toEntity(StudentDTO dto);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "schoolClass", ignore = true)
    @Mapping(target = "school", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(StudentDTO dto, @MappingTarget Student entity);
}
