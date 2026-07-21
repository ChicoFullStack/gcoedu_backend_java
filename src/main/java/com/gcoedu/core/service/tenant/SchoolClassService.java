package com.gcoedu.core.service.tenant;

import com.gcoedu.core.domain.dto.tenant.SchoolClassDTO;
import com.gcoedu.core.domain.entity.tenant.SchoolClass;
import com.gcoedu.core.mapper.tenant.SchoolClassMapper;
import com.gcoedu.core.repository.tenant.ClassRepository;
import com.gcoedu.core.repository.tenant.SchoolRepository;
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
public class SchoolClassService {

    private final ClassRepository classRepository;
    private final SchoolClassMapper schoolClassMapper;
    private final SchoolRepository schoolRepository;
    private final StudentRepository studentRepository;
    private final PermissionService permissionService;

    public SchoolClassService(ClassRepository classRepository, SchoolClassMapper schoolClassMapper,
                              SchoolRepository schoolRepository, StudentRepository studentRepository,
                              PermissionService permissionService) {
        this.classRepository = classRepository;
        this.schoolClassMapper = schoolClassMapper;
        this.schoolRepository = schoolRepository;
        this.studentRepository = studentRepository;
        this.permissionService = permissionService;
    }

    public void addStudents(String classId, List<String> studentIds) {
        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada"));
        
        if (!permissionService.canAccessClass(schoolClass)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado a esta turma.");
        }

        List<com.gcoedu.core.domain.entity.tenant.Student> students = studentRepository.findAllById(studentIds);
        for (com.gcoedu.core.domain.entity.tenant.Student student : students) {
            student.setSchoolClass(schoolClass);
        }
        studentRepository.saveAll(students);
    }

    public List<SchoolClassDTO> findAll() {
        return classRepository.findAll().stream()
                .filter(permissionService::canAccessClass)
                .map(schoolClassMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<SchoolClassDTO> findBySchoolId(String schoolId) {
        if (!permissionService.canAccessSchool(schoolId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado a esta escola.");
        }
        return classRepository.findBySchoolId(schoolId).stream()
                .map(schoolClassMapper::toDto)
                .collect(Collectors.toList());
    }

    public SchoolClassDTO findById(String id) {
        SchoolClass schoolClass = classRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada"));
        
        if (!permissionService.canAccessClass(schoolClass)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado a esta turma.");
        }
        return schoolClassMapper.toDto(schoolClass);
    }

    public SchoolClassDTO create(SchoolClassDTO dto) {
        if (dto.schoolId() != null && !permissionService.canAccessSchool(dto.schoolId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado para criar turma nesta escola.");
        }
        SchoolClass schoolClass = schoolClassMapper.toEntity(dto);
        if (dto.schoolId() != null) {
            schoolClass.setSchool(schoolRepository.findById(dto.schoolId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Escola não encontrada")));
        }
        if (schoolClass.getId() == null) {
            schoolClass.setId(java.util.UUID.randomUUID().toString());
        }
        SchoolClass saved = classRepository.save(schoolClass);
        return schoolClassMapper.toDto(saved);
    }

    public SchoolClassDTO update(String id, SchoolClassDTO dto) {
        SchoolClass schoolClass = classRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada"));
        
        if (!permissionService.canAccessClass(schoolClass)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado a esta turma.");
        }

        if (dto.schoolId() != null && !permissionService.canAccessSchool(dto.schoolId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado para mover turma para esta escola.");
        }

        schoolClassMapper.updateEntity(dto, schoolClass);
        if (dto.schoolId() != null) {
            schoolClass.setSchool(schoolRepository.findById(dto.schoolId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Escola não encontrada")));
        }
        
        SchoolClass saved = classRepository.save(schoolClass);
        return schoolClassMapper.toDto(saved);
    }

    public void delete(String id) {
        SchoolClass schoolClass = classRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada"));
        
        if (!permissionService.canAccessClass(schoolClass)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado a esta turma.");
        }
        classRepository.deleteById(id);
    }
}
