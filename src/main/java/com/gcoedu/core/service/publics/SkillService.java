package com.gcoedu.core.service.publics;

import com.gcoedu.core.domain.dto.publics.SkillBatchErrorDTO;
import com.gcoedu.core.domain.dto.publics.SkillBatchRequestDTO;
import com.gcoedu.core.domain.dto.publics.SkillBatchResponseDTO;
import com.gcoedu.core.domain.dto.publics.SkillCreateDTO;
import com.gcoedu.core.domain.dto.publics.SkillDTO;
import com.gcoedu.core.domain.entity.publics.Grade;
import com.gcoedu.core.domain.entity.publics.Skill;
import com.gcoedu.core.domain.entity.publics.Subject;
import com.gcoedu.core.repository.publics.GradeRepository;
import com.gcoedu.core.repository.publics.SkillRepository;
import com.gcoedu.core.repository.publics.SubjectRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SkillService {

    private final SkillRepository skillRepository;
    private final SubjectRepository subjectRepository;
    private final GradeRepository gradeRepository;

    public SkillService(
            SkillRepository skillRepository,
            SubjectRepository subjectRepository,
            GradeRepository gradeRepository
    ) {
        this.skillRepository = skillRepository;
        this.subjectRepository = subjectRepository;
        this.gradeRepository = gradeRepository;
    }

    public List<SkillDTO> findAll(String subjectId, UUID gradeId) {
        List<Skill> skills;
        if (hasText(subjectId) && gradeId != null) {
            skills = skillRepository.findDistinctBySubjectIdAndGradesIdOrderByCodeAsc(subjectId.trim(), gradeId);
        } else if (hasText(subjectId)) {
            skills = skillRepository.findBySubjectIdOrderByCodeAsc(subjectId.trim());
        } else if (gradeId != null) {
            skills = skillRepository.findDistinctByGradesIdOrderByCodeAsc(gradeId);
        } else {
            skills = skillRepository.findAllByOrderByCodeAsc();
        }
        return skills.stream().map(this::toDto).toList();
    }

    @Transactional
    public SkillDTO create(SkillCreateDTO request) {
        String code = normalizeCode(request.getCode());
        ensureCodeIsAvailable(code);
        return toDto(skillRepository.save(buildSkill(request, code)));
    }

    @Transactional
    public SkillBatchResponseDTO createBatch(SkillBatchRequestDTO request) {
        List<SkillDTO> created = new ArrayList<>();
        List<SkillBatchErrorDTO> errors = new ArrayList<>();
        Set<String> batchCodes = new LinkedHashSet<>();

        for (int index = 0; index < request.getSkills().size(); index++) {
            SkillCreateDTO item = request.getSkills().get(index);
            try {
                String code = normalizeCode(item.getCode());
                if (!batchCodes.add(code.toLowerCase(Locale.ROOT))) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Código repetido no lote: " + code);
                }
                ensureCodeIsAvailable(code);
                created.add(toDto(skillRepository.save(buildSkill(item, code))));
            } catch (ResponseStatusException | IllegalArgumentException ex) {
                String message = ex instanceof ResponseStatusException responseStatusException
                        ? responseStatusException.getReason()
                        : ex.getMessage();
                errors.add(new SkillBatchErrorDTO(index, message == null ? "Item inválido" : message));
            }
        }

        if (created.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nenhuma habilidade válida para criar");
        }

        return new SkillBatchResponseDTO(created, created.size(), errors);
    }

    @Transactional
    public void delete(UUID id) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Habilidade não encontrada"));
        try {
            skillRepository.delete(skill);
            skillRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Não é possível excluir esta habilidade porque ela está em uso",
                    ex
            );
        }
    }

    private Skill buildSkill(SkillCreateDTO request, String code) {
        Skill skill = new Skill();
        skill.setId(UUID.randomUUID());
        skill.setCode(code);
        skill.setDescription(normalizeDescription(request.getDescription()));
        skill.setSubject(resolveSubject(request.getSubjectId()));
        skill.setGrades(resolveGrades(request));
        return skill;
    }

    private Subject resolveSubject(String subjectId) {
        if (!hasText(subjectId)) {
            return null;
        }
        return subjectRepository.findById(subjectId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Disciplina não encontrada"));
    }

    private List<Grade> resolveGrades(SkillCreateDTO request) {
        LinkedHashSet<UUID> ids = new LinkedHashSet<>();
        if (request.getGradeIds() != null) {
            ids.addAll(request.getGradeIds());
        }
        if (request.getGradeId() != null) {
            ids.add(request.getGradeId());
        }
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }

        List<Grade> grades = gradeRepository.findAllById(ids);
        if (grades.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uma ou mais séries não foram encontradas");
        }
        return new ArrayList<>(grades);
    }

    private void ensureCodeIsAvailable(String code) {
        skillRepository.findByCodeIgnoreCase(code).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma habilidade com o código " + code);
        });
    }

    private SkillDTO toDto(Skill skill) {
        List<UUID> gradeIds = skill.getGrades() == null
                ? List.of()
                : skill.getGrades().stream().map(Grade::getId).toList();
        return new SkillDTO(
                skill.getId(),
                skill.getCode(),
                skill.getDescription(),
                skill.getSubject() == null ? null : skill.getSubject().getId(),
                gradeIds
        );
    }

    private String normalizeCode(String value) {
        if (!hasText(value)) {
            throw new IllegalArgumentException("O código da habilidade é obrigatório");
        }
        return value.trim().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private String normalizeDescription(String value) {
        if (!hasText(value)) {
            throw new IllegalArgumentException("A descrição da habilidade é obrigatória");
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
