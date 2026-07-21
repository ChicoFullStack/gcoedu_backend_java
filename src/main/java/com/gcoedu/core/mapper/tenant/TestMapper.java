package com.gcoedu.core.mapper.tenant;

import com.gcoedu.core.domain.dto.tenant.TestDTO;
import com.gcoedu.core.domain.entity.tenant.Test;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TestMapper {
    @Mapping(target = "creatorId", source = "creator.id")
    @Mapping(target = "subjectId", source = "subjectRel.id")
    @Mapping(target = "grade", source = "gradeId")
    TestDTO toDto(Test test);

    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "subjectRel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "gradeId", source = "grade")
    Test toEntity(TestDTO dto);

    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "subjectRel", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "gradeId", source = "grade")
    void updateEntity(TestDTO dto, @MappingTarget Test entity);
}
