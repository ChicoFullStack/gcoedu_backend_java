package com.gcoedu.core.service.publics;

import com.gcoedu.core.domain.dto.publics.SubjectDTO;
import com.gcoedu.core.domain.entity.publics.Subject;
import com.gcoedu.core.mapper.publics.SubjectMapper;
import com.gcoedu.core.repository.publics.SubjectRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final SubjectMapper subjectMapper;

    public SubjectService(SubjectRepository subjectRepository, SubjectMapper subjectMapper) {
        this.subjectRepository = subjectRepository;
        this.subjectMapper = subjectMapper;
    }

    public List<SubjectDTO> findAll() {
        return subjectRepository.findAll().stream()
                .map(subjectMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public SubjectDTO create(SubjectDTO dto) {
        String normalizedName = normalizeName(dto.getName());
        ensureNameIsAvailable(normalizedName, null);

        Subject subject = new Subject();
        subject.setId(UUID.randomUUID().toString());
        subject.setName(normalizedName);

        return subjectMapper.toDto(subjectRepository.save(subject));
    }

    @Transactional
    public SubjectDTO update(String id, SubjectDTO dto) {
        Subject subject = findById(id);
        String normalizedName = normalizeName(dto.getName());
        ensureNameIsAvailable(normalizedName, id);

        subject.setName(normalizedName);
        return subjectMapper.toDto(subjectRepository.save(subject));
    }

    @Transactional
    public void delete(String id) {
        Subject subject = findById(id);

        try {
            subjectRepository.delete(subject);
            subjectRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Não é possível excluir esta disciplina porque ela está em uso.",
                    ex
            );
        }
    }

    private Subject findById(String id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Disciplina não encontrada"
                ));
    }

    private void ensureNameIsAvailable(String name, String currentSubjectId) {
        subjectRepository.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(currentSubjectId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Já existe uma disciplina com este nome"
                    );
                });
    }

    private String normalizeName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }
}
