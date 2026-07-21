package com.gcoedu.core.service.publics;

import com.gcoedu.core.domain.dto.publics.GradeDTO;
import com.gcoedu.core.domain.entity.publics.Grade;
import com.gcoedu.core.mapper.publics.GradeMapper;
import com.gcoedu.core.repository.publics.GradeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GradeService {

    private final GradeRepository gradeRepository;
    private final GradeMapper gradeMapper;

    public GradeService(GradeRepository gradeRepository, GradeMapper gradeMapper) {
        this.gradeRepository = gradeRepository;
        this.gradeMapper = gradeMapper;
    }

    public List<GradeDTO> findAll() {
        return gradeRepository.findAll().stream()
                .map(gradeMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<GradeDTO> findByEducationStageId(java.util.UUID educationStageId) {
        return gradeRepository.findByEducationStageId(educationStageId).stream()
                .map(gradeMapper::toDto)
                .collect(Collectors.toList());
    }
}
