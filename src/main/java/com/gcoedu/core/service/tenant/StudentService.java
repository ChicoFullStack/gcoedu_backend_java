package com.gcoedu.core.service.tenant;

import com.gcoedu.core.domain.dto.tenant.StudentDTO;
import com.gcoedu.core.domain.entity.tenant.Student;
import com.gcoedu.core.mapper.tenant.StudentMapper;
import com.gcoedu.core.repository.tenant.StudentRepository;
import com.gcoedu.core.service.PermissionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class StudentService {

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;
    private final PermissionService permissionService;

    public StudentService(StudentRepository studentRepository, StudentMapper studentMapper,
                          PermissionService permissionService) {
        this.studentRepository = studentRepository;
        this.studentMapper = studentMapper;
        this.permissionService = permissionService;
    }

    public List<StudentDTO> findAll() {
        return studentRepository.findAll().stream()
                .filter(permissionService::canAccessStudent)
                .map(studentMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<StudentDTO> findBySchoolClassId(String schoolClassId) {
        return studentRepository.findBySchoolClassId(schoolClassId).stream()
                .filter(permissionService::canAccessStudent)
                .map(studentMapper::toDto)
                .collect(Collectors.toList());
    }

    public StudentDTO findById(String id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno não encontrado"));
        
        if (!permissionService.canAccessStudent(student)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado a este aluno.");
        }
        return studentMapper.toDto(student);
    }

    public StudentDTO create(StudentDTO dto) {
        if (dto.schoolId() != null && !permissionService.canAccessSchool(dto.schoolId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado para criar aluno nesta escola.");
        }
        if (studentRepository.existsByRegistration(dto.registration())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Matrícula já existente");
        }
        Student student = studentMapper.toEntity(dto);
        if (student.getId() == null) {
            student.setId(java.util.UUID.randomUUID().toString());
        }
        Student saved = studentRepository.save(student);
        return studentMapper.toDto(saved);
    }

    public StudentDTO update(String id, StudentDTO dto) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno não encontrado"));
        
        if (!permissionService.canAccessStudent(student)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado a este aluno.");
        }

        if (dto.schoolId() != null && !permissionService.canAccessSchool(dto.schoolId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado para mover aluno para esta escola.");
        }

        studentMapper.updateEntity(dto, student);
        
        Student saved = studentRepository.save(student);
        return studentMapper.toDto(saved);
    }

    public void delete(String id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno não encontrado"));
        
        if (!permissionService.canAccessStudent(student)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado a este aluno.");
        }
        studentRepository.deleteById(id);
    }
}
