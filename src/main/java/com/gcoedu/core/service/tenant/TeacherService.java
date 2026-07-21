package com.gcoedu.core.service.tenant;

import com.gcoedu.core.domain.dto.tenant.TeacherDTO;
import com.gcoedu.core.domain.entity.tenant.Teacher;
import com.gcoedu.core.mapper.tenant.TeacherMapper;
import com.gcoedu.core.repository.tenant.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final TeacherMapper teacherMapper;

    public TeacherService(TeacherRepository teacherRepository, TeacherMapper teacherMapper) {
        this.teacherRepository = teacherRepository;
        this.teacherMapper = teacherMapper;
    }

    public List<TeacherDTO> findAll() {
        return teacherRepository.findAll().stream()
                .map(teacherMapper::toDto)
                .collect(Collectors.toList());
    }

    public TeacherDTO findById(String id) {
        return teacherRepository.findById(id)
                .map(teacherMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Professor não encontrado"));
    }

    public TeacherDTO create(TeacherDTO dto) {
        Teacher teacher = teacherMapper.toEntity(dto);
        if (teacher.getId() == null) {
            teacher.setId(java.util.UUID.randomUUID().toString());
        }
        // FIXME: attach User if provided
        Teacher saved = teacherRepository.save(teacher);
        return teacherMapper.toDto(saved);
    }

    public TeacherDTO update(String id, TeacherDTO dto) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Professor não encontrado"));
        
        teacherMapper.updateEntity(dto, teacher);
        
        Teacher saved = teacherRepository.save(teacher);
        return teacherMapper.toDto(saved);
    }

    public void delete(String id) {
        teacherRepository.deleteById(id);
    }
}
