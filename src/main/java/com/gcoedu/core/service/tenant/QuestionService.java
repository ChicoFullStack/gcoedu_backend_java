package com.gcoedu.core.service.tenant;

import com.gcoedu.core.domain.dto.tenant.QuestionBulkDeleteResponse;
import com.gcoedu.core.domain.dto.tenant.QuestionDTO;
import com.gcoedu.core.domain.dto.tenant.QuestionOptionDTO;
import com.gcoedu.core.domain.dto.tenant.QuestionPermissionsDTO;
import com.gcoedu.core.domain.dto.tenant.QuestionReferenceDTO;
import com.gcoedu.core.domain.dto.tenant.QuestionUpsertRequest;
import com.gcoedu.core.domain.entity.publics.EducationStage;
import com.gcoedu.core.domain.entity.publics.Grade;
import com.gcoedu.core.domain.entity.publics.RoleEnum;
import com.gcoedu.core.domain.entity.publics.Skill;
import com.gcoedu.core.domain.entity.publics.Subject;
import com.gcoedu.core.domain.entity.publics.User;
import com.gcoedu.core.domain.entity.tenant.Question;
import com.gcoedu.core.domain.entity.tenant.QuestionScopeType;
import com.gcoedu.core.repository.publics.EducationStageRepository;
import com.gcoedu.core.repository.publics.GradeRepository;
import com.gcoedu.core.repository.publics.SkillRepository;
import com.gcoedu.core.repository.publics.SubjectRepository;
import com.gcoedu.core.repository.tenant.QuestionRepository;
import com.gcoedu.core.repository.tenant.StudentAnswerRepository;
import com.gcoedu.core.repository.tenant.TestQuestionRepository;
import com.gcoedu.core.service.PermissionService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class QuestionService {

    private static final Set<RoleEnum> QUESTION_ROLES = Set.of(
            RoleEnum.ADMIN,
            RoleEnum.TECADM,
            RoleEnum.DIRETOR,
            RoleEnum.COORDENADOR,
            RoleEnum.PROFESSOR
    );

    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final GradeRepository gradeRepository;
    private final EducationStageRepository educationStageRepository;
    private final SkillRepository skillRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final PermissionService permissionService;

    public QuestionService(
            QuestionRepository questionRepository,
            SubjectRepository subjectRepository,
            GradeRepository gradeRepository,
            EducationStageRepository educationStageRepository,
            SkillRepository skillRepository,
            TestQuestionRepository testQuestionRepository,
            StudentAnswerRepository studentAnswerRepository,
            PermissionService permissionService
    ) {
        this.questionRepository = questionRepository;
        this.subjectRepository = subjectRepository;
        this.gradeRepository = gradeRepository;
        this.educationStageRepository = educationStageRepository;
        this.skillRepository = skillRepository;
        this.testQuestionRepository = testQuestionRepository;
        this.studentAnswerRepository = studentAnswerRepository;
        this.permissionService = permissionService;
    }

    @Transactional(readOnly = true)
    public List<QuestionDTO> findAll(String createdById, String subjectId, String questionType) {
        User currentUser = requireQuestionUser();
        String cityId = currentUser.getCity() == null ? null : currentUser.getCity().getId();

        return questionRepository.findAccessible(
                        QuestionScopeType.GLOBAL.name(),
                        QuestionScopeType.CITY.name(),
                        QuestionScopeType.PRIVATE.name(),
                        cityId,
                        currentUser.getId(),
                        blankToNull(createdById),
                        blankToNull(subjectId),
                        blankToNull(questionType)
                ).stream()
                .map(question -> toDto(question, currentUser))
                .toList();
    }

    @Transactional(readOnly = true)
    public QuestionDTO findById(String id) {
        User currentUser = requireQuestionUser();
        Question question = getVisibleQuestion(id, currentUser);
        return toDto(question, currentUser);
    }

    public QuestionDTO create(QuestionUpsertRequest request) {
        User currentUser = requireQuestionUser();
        Question question = new Question();
        question.setId(UUID.randomUUID().toString());
        question.setNumber(questionRepository.findMaxNumber() + 1);
        question.setCreatedBy(currentUser);
        question.setLastModifiedBy(currentUser);
        question.setCreatedAt(java.time.LocalDateTime.now());
        question.setUpdatedAt(java.time.LocalDateTime.now());
        applyCreationScope(question, currentUser);
        applyRequest(question, request);

        return toDto(questionRepository.saveAndFlush(question), currentUser);
    }

    public QuestionDTO update(String id, QuestionUpsertRequest request) {
        User currentUser = requireQuestionUser();
        Question question = getVisibleQuestion(id, currentUser);
        assertCanMutate(question, currentUser);
        applyRequest(question, request);
        question.setLastModifiedBy(currentUser);
        question.setVersion(question.getVersion() == null ? 2 : question.getVersion() + 1);

        return toDto(questionRepository.saveAndFlush(question), currentUser);
    }

    public void delete(String id) {
        User currentUser = requireQuestionUser();
        deleteAuthorized(id, currentUser);
    }

    public QuestionBulkDeleteResponse deleteBulk(List<String> ids) {
        User currentUser = requireQuestionUser();
        List<String> uniqueIds = new ArrayList<>(new LinkedHashSet<>(ids));
        if (uniqueIds.size() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exclua no máximo 100 questões por vez");
        }

        // Valida o lote inteiro antes de remover qualquer registro.
        List<Question> questions = uniqueIds.stream()
                .map(id -> getVisibleQuestion(id, currentUser))
                .toList();
        questions.forEach(question -> {
            assertCanMutate(question, currentUser);
            assertNotInUse(question.getId());
        });

        try {
            questionRepository.deleteAll(questions);
            questionRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Uma ou mais questões estão vinculadas a avaliações ou respostas",
                    exception
            );
        }
        return new QuestionBulkDeleteResponse(questions.size());
    }

    private void deleteAuthorized(String id, User currentUser) {
        Question question = getVisibleQuestion(id, currentUser);
        assertCanMutate(question, currentUser);
        assertNotInUse(id);
        try {
            questionRepository.delete(question);
            questionRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A questão está vinculada a uma avaliação ou resposta",
                    exception
            );
        }
    }

    private void assertNotInUse(String questionId) {
        if (testQuestionRepository.countByQuestionId(questionId) > 0
                || studentAnswerRepository.existsByQuestionId(questionId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A questão está vinculada a uma avaliação ou resposta e não pode ser excluída"
            );
        }
    }

    private void applyCreationScope(Question question, User user) {
        if (user.getRole() == RoleEnum.ADMIN) {
            question.setScopeType(QuestionScopeType.GLOBAL.name());
            return;
        }
        if (user.getRole() == RoleEnum.TECADM) {
            String cityId = user.getCity() == null ? null : user.getCity().getId();
            if (cityId == null) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "O técnico administrador precisa estar vinculado a um município"
                );
            }
            question.setScopeType(QuestionScopeType.CITY.name());
            question.setOwnerCityId(cityId);
            return;
        }
        question.setScopeType(QuestionScopeType.PRIVATE.name());
        question.setOwnerUserId(user.getId());
    }

    private void applyRequest(Question question, QuestionUpsertRequest request) {
        String questionType = normalizeQuestionType(request.type());
        String difficulty = normalizeDifficulty(request.difficulty());
        if (request.value() == null || !Double.isFinite(request.value()) || request.value() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O valor da questão deve ser um número maior ou igual a zero"
            );
        }

        Subject subject = subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Disciplina não encontrada"));
        Grade grade = gradeRepository.findById(parseUuid(request.grade(), "Série inválida"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Série não encontrada"));

        EducationStage educationStage = grade.getEducationStage();
        if (request.educationStageId() != null && !request.educationStageId().isBlank()) {
            EducationStage requestedStage = educationStageRepository
                    .findById(parseUuid(request.educationStageId(), "Etapa de ensino inválida"))
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Etapa de ensino não encontrada"
                    ));
            if (educationStage == null || !Objects.equals(educationStage.getId(), requestedStage.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A série não pertence à etapa de ensino informada"
                );
            }
            educationStage = requestedStage;
        }

        List<String> skills = sanitizeIds(request.skills());
        validateSkills(skills, subject, grade);
        List<QuestionOptionDTO> options = isMultipleChoice(questionType)
                ? sanitizeOptions(request.options())
                : List.of();
        validateOptions(questionType, options);

        String solution = normalizedSolution(questionType, request.solution(), options);
        question.setTitle(request.title().trim());
        question.setDescription(blankToNull(request.description()));
        question.setText(request.text().trim());
        question.setFormattedText(blankToNull(request.formattedText()));
        question.setSecondStatement(blankToNull(request.secondStatement()));
        question.setQuestionType(questionType);
        question.setSubject(subject);
        question.setGrade(grade);
        question.setEducationStage(educationStage);
        question.setDifficultyLevel(difficulty);
        question.setValue(request.value());
        question.setCorrectAnswer(solution);
        question.setFormattedSolution(blankToNull(request.formattedSolution()));
        question.setAlternatives(toAlternativeMaps(options));
        question.setSkill(String.join(",", skills));
        question.setTopics(request.topics() == null ? List.of() : request.topics());
        question.setVersion(question.getVersion() == null ? 1 : question.getVersion());

        // Colunas legadas permanecem obrigatórias no banco.
        question.setStatement(request.text().trim());
        question.setCorrectOption(isLetterAnswer(solution) ? solution.toUpperCase(Locale.ROOT) : "A");
        setLegacyOptions(question, options);
    }

    private void validateSkills(List<String> skillIds, Subject subject, Grade grade) {
        if (skillIds.isEmpty()) {
            return;
        }
        List<UUID> ids = skillIds.stream()
                .map(id -> parseUuid(id, "Habilidade inválida"))
                .toList();
        List<Skill> skills = skillRepository.findAllById(ids);
        if (skills.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uma ou mais habilidades não foram encontradas");
        }
        boolean invalid = skills.stream().anyMatch(skill ->
                (skill.getSubject() != null && !Objects.equals(skill.getSubject().getId(), subject.getId()))
                        || (skill.getGrades() != null && !skill.getGrades().isEmpty()
                        && skill.getGrades().stream().noneMatch(item -> item.getId().equals(grade.getId())))
        );
        if (invalid) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Uma ou mais habilidades não pertencem à disciplina e série informadas"
            );
        }
    }

    private void validateOptions(String type, List<QuestionOptionDTO> options) {
        if (!isMultipleChoice(type)) {
            return;
        }
        if (options.size() < 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Questões de múltipla escolha exigem ao menos duas alternativas"
            );
        }
        long correct = options.stream().filter(QuestionOptionDTO::isCorrect).count();
        if (correct != 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Marque exatamente uma alternativa como correta"
            );
        }
    }

    private String normalizedSolution(String type, String requestedSolution, List<QuestionOptionDTO> options) {
        if (isMultipleChoice(type)) {
            int correctIndex = -1;
            for (int index = 0; index < options.size(); index++) {
                if (options.get(index).isCorrect()) {
                    correctIndex = index;
                    break;
                }
            }
            return String.valueOf((char) ('A' + correctIndex));
        }
        return blankToNull(requestedSolution);
    }

    private List<QuestionOptionDTO> sanitizeOptions(List<QuestionOptionDTO> options) {
        if (options == null) {
            return List.of();
        }
        List<QuestionOptionDTO> result = new ArrayList<>();
        for (int index = 0; index < options.size(); index++) {
            QuestionOptionDTO option = options.get(index);
            if (option == null) {
                continue;
            }
            String text = option.text() == null ? "" : option.text().trim();
            if (text.isEmpty() && option.image() == null) {
                continue;
            }
            // O identificador e o gabarito são sempre derivados da ordem já
            // sanitizada; o cliente não controla letras ou índices.
            String id = String.valueOf((char) ('A' + result.size()));
            result.add(new QuestionOptionDTO(id, text, option.isCorrect(), option.image()));
        }
        return result;
    }

    private List<Map<String, Object>> toAlternativeMaps(List<QuestionOptionDTO> options) {
        return options.stream().map(option -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", option.id());
            map.put("text", option.text());
            map.put("isCorrect", option.isCorrect());
            if (option.image() != null) {
                map.put("image", option.image());
            }
            return map;
        }).toList();
    }

    private void setLegacyOptions(Question question, List<QuestionOptionDTO> options) {
        List<String> texts = options.stream().map(QuestionOptionDTO::text).toList();
        question.setOptionA(valueAt(texts, 0));
        question.setOptionB(valueAt(texts, 1));
        question.setOptionC(valueAt(texts, 2));
        question.setOptionD(valueAt(texts, 3));
        question.setOptionE(valueAt(texts, 4));
    }

    private String valueAt(List<String> values, int index) {
        return index < values.size() ? blankToNull(values.get(index)) : null;
    }

    private Question getVisibleQuestion(String id, User currentUser) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Questão não encontrada"));
        if (!canView(question, currentUser)) {
            // Não revela a existência de conteúdo de outro tenant/usuário.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Questão não encontrada");
        }
        return question;
    }

    private boolean canView(Question question, User user) {
        QuestionScopeType scope = effectiveScope(question);
        if (scope == QuestionScopeType.GLOBAL) {
            return true;
        }
        if (scope == QuestionScopeType.CITY) {
            String cityId = user.getCity() == null ? null : user.getCity().getId();
            return cityId != null && cityId.equals(question.getOwnerCityId());
        }
        return user.getId().equals(question.getOwnerUserId());
    }

    private boolean canMutate(Question question, User user) {
        QuestionScopeType scope = effectiveScope(question);
        if (scope == QuestionScopeType.GLOBAL) {
            return user.getRole() == RoleEnum.ADMIN;
        }
        if (scope == QuestionScopeType.CITY) {
            String cityId = user.getCity() == null ? null : user.getCity().getId();
            return cityId != null
                    && cityId.equals(question.getOwnerCityId())
                    && (user.getRole() == RoleEnum.ADMIN || user.getRole() == RoleEnum.TECADM);
        }
        return user.getId().equals(question.getOwnerUserId());
    }

    private void assertCanMutate(Question question, User user) {
        if (!canMutate(question, user)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Você não tem permissão para alterar esta questão"
            );
        }
    }

    private QuestionScopeType effectiveScope(Question question) {
        if (question.getScopeType() == null || question.getScopeType().isBlank()) {
            return QuestionScopeType.GLOBAL;
        }
        try {
            return QuestionScopeType.valueOf(question.getScopeType().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "A questão possui um escopo inválido"
            );
        }
    }

    private User requireQuestionUser() {
        User user = permissionService.getCurrentUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }
        if (!QUESTION_ROLES.contains(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Perfil sem acesso ao banco de questões");
        }
        return user;
    }

    private QuestionDTO toDto(Question question, User currentUser) {
        List<QuestionOptionDTO> options = readOptions(question);
        String text = firstNonBlank(question.getText(), question.getStatement(), "");
        String title = firstNonBlank(question.getTitle(), text, "Questão");
        String type = firstNonBlank(
                question.getQuestionType(),
                options.isEmpty() ? "dissertativa" : "multipleChoice"
        );
        String solution = firstNonBlank(question.getCorrectAnswer(), question.getCorrectOption(), "");
        QuestionScopeType scope = effectiveScope(question);
        boolean canMutate = canMutate(question, currentUser);

        return new QuestionDTO(
                question.getId(),
                question.getNumber() == null ? 1 : question.getNumber(),
                title,
                firstNonBlank(question.getDescription(), ""),
                text,
                firstNonBlank(question.getFormattedText(), text),
                firstNonBlank(question.getSecondStatement(), ""),
                type,
                firstNonBlank(question.getDifficultyLevel(), "Médio"),
                question.getValue() == null ? 1D : question.getValue(),
                solution,
                solution,
                firstNonBlank(question.getFormattedSolution(), ""),
                options,
                options,
                splitSkills(question.getSkill()),
                question.getTopics() == null ? List.of() : question.getTopics(),
                question.getVersion() == null ? 1 : question.getVersion(),
                question.getSubject() == null ? null : question.getSubject().getId(),
                reference(question.getSubject()),
                reference(question.getGrade()),
                reference(question.getEducationStage()),
                question.getCreatedAt(),
                question.getUpdatedAt(),
                reference(question.getCreatedBy()),
                reference(question.getLastModifiedBy()),
                scope.name(),
                new QuestionPermissionsDTO(canMutate, canMutate)
        );
    }

    private List<QuestionOptionDTO> readOptions(Question question) {
        if (question.getAlternatives() != null && !question.getAlternatives().isEmpty()) {
            List<QuestionOptionDTO> result = new ArrayList<>();
            for (int index = 0; index < question.getAlternatives().size(); index++) {
                Map<String, Object> item = question.getAlternatives().get(index);
                String id = Objects.toString(item.getOrDefault("id", String.valueOf((char) ('A' + index))));
                String text = Objects.toString(item.getOrDefault("text", ""));
                Object correctValue = item.containsKey("isCorrect")
                        ? item.get("isCorrect")
                        : item.get("is_correct");
                boolean isCorrect = Boolean.TRUE.equals(correctValue)
                        || "true".equalsIgnoreCase(Objects.toString(correctValue, "false"));
                if (!isCorrect && isLetterAnswer(question.getCorrectAnswer())) {
                    isCorrect = question.getCorrectAnswer().equalsIgnoreCase(
                            String.valueOf((char) ('A' + index))
                    );
                }
                result.add(new QuestionOptionDTO(id, text, isCorrect, item.get("image")));
            }
            return result;
        }

        List<String> legacy = List.of(
                nullToEmpty(question.getOptionA()),
                nullToEmpty(question.getOptionB()),
                nullToEmpty(question.getOptionC()),
                nullToEmpty(question.getOptionD()),
                nullToEmpty(question.getOptionE())
        );
        List<QuestionOptionDTO> result = new ArrayList<>();
        for (int index = 0; index < legacy.size(); index++) {
            String text = legacy.get(index);
            if (text.isBlank()) {
                continue;
            }
            String letter = String.valueOf((char) ('A' + index));
            result.add(new QuestionOptionDTO(
                    letter,
                    text,
                    letter.equalsIgnoreCase(question.getCorrectOption()),
                    null
            ));
        }
        return result;
    }

    private QuestionReferenceDTO reference(Subject subject) {
        return subject == null ? null : new QuestionReferenceDTO(subject.getId(), subject.getName());
    }

    private QuestionReferenceDTO reference(Grade grade) {
        return grade == null ? null : new QuestionReferenceDTO(grade.getId().toString(), grade.getName());
    }

    private QuestionReferenceDTO reference(EducationStage stage) {
        return stage == null ? null : new QuestionReferenceDTO(stage.getId().toString(), stage.getName());
    }

    private QuestionReferenceDTO reference(User user) {
        return user == null ? null : new QuestionReferenceDTO(user.getId(), user.getName());
    }

    private List<String> splitSkills(String skills) {
        if (skills == null || skills.isBlank()) {
            return List.of();
        }
        return sanitizeIds(List.of(skills.split(",")));
    }

    private List<String> sanitizeIds(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }

    private UUID parseUuid(String value, String message) {
        try {
            return UUID.fromString(value);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private boolean isMultipleChoice(String type) {
        if (type == null) {
            return false;
        }
        String normalized = type.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
        return normalized.equals("multiplechoice") || normalized.equals("multiplaescolha");
    }

    private String normalizeQuestionType(String type) {
        if (type == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O tipo da questão é obrigatório");
        }
        if (isMultipleChoice(type)) {
            return "multipleChoice";
        }
        if ("dissertativa".equalsIgnoreCase(type.trim())) {
            return "dissertativa";
        }
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "O tipo da questão deve ser multipleChoice ou dissertativa"
        );
    }

    private String normalizeDifficulty(String difficulty) {
        if (difficulty == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A dificuldade é obrigatória");
        }
        String normalized = difficulty.trim();
        if (Set.of("Abaixo do Básico", "Básico", "Adequado", "Avançado").contains(normalized)) {
            return normalized;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A dificuldade informada é inválida");
    }

    private boolean isLetterAnswer(String value) {
        return value != null && value.matches("(?i)[A-E]");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
