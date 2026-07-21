package com.gcoedu.core.service.tenant;

import com.gcoedu.core.domain.dto.tenant.QuestionDTO;
import com.gcoedu.core.domain.dto.tenant.QuestionOptionDTO;
import com.gcoedu.core.domain.dto.tenant.QuestionUpsertRequest;
import com.gcoedu.core.domain.entity.publics.City;
import com.gcoedu.core.domain.entity.publics.EducationStage;
import com.gcoedu.core.domain.entity.publics.Grade;
import com.gcoedu.core.domain.entity.publics.RoleEnum;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock QuestionRepository questionRepository;
    @Mock SubjectRepository subjectRepository;
    @Mock GradeRepository gradeRepository;
    @Mock EducationStageRepository educationStageRepository;
    @Mock SkillRepository skillRepository;
    @Mock TestQuestionRepository testQuestionRepository;
    @Mock StudentAnswerRepository studentAnswerRepository;
    @Mock PermissionService permissionService;

    private QuestionService service;
    private Subject subject;
    private Grade grade;
    private EducationStage stage;

    @BeforeEach
    void setUp() {
        service = new QuestionService(
                questionRepository,
                subjectRepository,
                gradeRepository,
                educationStageRepository,
                skillRepository,
                testQuestionRepository,
                studentAnswerRepository,
                permissionService
        );

        subject = new Subject();
        subject.setId("subject-id");
        subject.setName("Matemática");

        stage = new EducationStage();
        stage.setId(UUID.randomUUID());
        stage.setName("Ensino Fundamental");

        grade = new Grade();
        grade.setId(UUID.randomUUID());
        grade.setName("8º Ano");
        grade.setEducationStage(stage);
    }

    @Test
    void createsCityQuestionForTecadmUsingAuthenticatedUserAndCity() {
        User user = user(RoleEnum.TECADM, "city-a");
        when(permissionService.getCurrentUser()).thenReturn(user);
        mockReferences();
        when(questionRepository.findMaxNumber()).thenReturn(10);
        when(questionRepository.saveAndFlush(any(Question.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        QuestionDTO result = service.create(request());

        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).saveAndFlush(captor.capture());
        Question saved = captor.getValue();
        assertThat(saved.getScopeType()).isEqualTo(QuestionScopeType.CITY.name());
        assertThat(saved.getOwnerCityId()).isEqualTo("city-a");
        assertThat(saved.getOwnerUserId()).isNull();
        assertThat(saved.getCreatedBy()).isSameAs(user);
        assertThat(saved.getLastModifiedBy()).isSameAs(user);
        assertThat(saved.getStatement()).isEqualTo("Quanto é 2 + 2?");
        assertThat(saved.getCorrectOption()).isEqualTo("A");
        assertThat(saved.getAlternatives())
                .extracting(alternative -> alternative.get("id"))
                .containsExactly("A", "B");
        assertThat(result.scopeType()).isEqualTo("CITY");
        assertThat(result.permissions().canEdit()).isTrue();
        assertThat(result.permissions().canDelete()).isTrue();
    }

    @Test
    void createsPrivateQuestionForProfessor() {
        User user = user(RoleEnum.PROFESSOR, "city-a");
        when(permissionService.getCurrentUser()).thenReturn(user);
        mockReferences();
        when(questionRepository.findMaxNumber()).thenReturn(0);
        when(questionRepository.saveAndFlush(any(Question.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.create(request());

        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getScopeType()).isEqualTo(QuestionScopeType.PRIVATE.name());
        assertThat(captor.getValue().getOwnerUserId()).isEqualTo(user.getId());
        assertThat(captor.getValue().getOwnerCityId()).isNull();
    }

    @Test
    void discardsAlternativesWhenCreatingEssayQuestion() {
        User user = user(RoleEnum.PROFESSOR, "city-a");
        when(permissionService.getCurrentUser()).thenReturn(user);
        mockReferences();
        when(questionRepository.findMaxNumber()).thenReturn(0);
        when(questionRepository.saveAndFlush(any(Question.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        QuestionUpsertRequest essayRequest = request(
                "dissertativa",
                "A resposta esperada",
                List.of(
                        new QuestionOptionDTO("A", "Alternativa indevida", true, null),
                        new QuestionOptionDTO("B", "Outra alternativa", false, null)
                ),
                1D,
                "Básico"
        );

        service.create(essayRequest);

        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).saveAndFlush(captor.capture());
        Question saved = captor.getValue();
        assertThat(saved.getQuestionType()).isEqualTo("dissertativa");
        assertThat(saved.getAlternatives()).isEmpty();
        assertThat(saved.getOptionA()).isNull();
        assertThat(saved.getOptionB()).isNull();
        assertThat(saved.getCorrectAnswer()).isEqualTo("A resposta esperada");
    }

    @Test
    void rejectsUnknownQuestionTypeBeforePersisting() {
        when(permissionService.getCurrentUser()).thenReturn(user(RoleEnum.ADMIN, "city-a"));

        assertThatThrownBy(() -> service.create(request(
                "tipo-inexistente",
                null,
                List.of(),
                1D,
                "Básico"
        )))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                    assertThat(exception.getReason())
                            .isEqualTo("O tipo da questão deve ser multipleChoice ou dissertativa");
                });

        verify(questionRepository, never()).saveAndFlush(any(Question.class));
    }

    @Test
    void rejectsNonFiniteQuestionValueBeforePersisting() {
        when(permissionService.getCurrentUser()).thenReturn(user(RoleEnum.ADMIN, "city-a"));

        assertThatThrownBy(() -> service.create(request(
                "dissertativa",
                null,
                List.of(),
                Double.NaN,
                "Básico"
        )))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                    assertThat(exception.getReason())
                            .isEqualTo("O valor da questão deve ser um número maior ou igual a zero");
                });

        verify(questionRepository, never()).saveAndFlush(any(Question.class));
    }

    @Test
    void tecadmCannotDeleteGlobalQuestion() {
        User user = user(RoleEnum.TECADM, "city-a");
        Question global = legacyQuestion();
        when(permissionService.getCurrentUser()).thenReturn(user);
        when(questionRepository.findById(global.getId())).thenReturn(Optional.of(global));

        assertThatThrownBy(() -> service.delete(global.getId()))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode().value()).isEqualTo(HttpStatus.FORBIDDEN.value());
                    assertThat(exception.getReason()).isEqualTo("Você não tem permissão para alterar esta questão");
                });

        verify(questionRepository, never()).delete(any(Question.class));
    }

    @Test
    void hidesQuestionOwnedByAnotherCity() {
        User user = user(RoleEnum.TECADM, "city-a");
        Question otherCity = legacyQuestion();
        otherCity.setScopeType(QuestionScopeType.CITY.name());
        otherCity.setOwnerCityId("city-b");
        when(permissionService.getCurrentUser()).thenReturn(user);
        when(questionRepository.findById(otherCity.getId())).thenReturn(Optional.of(otherCity));

        assertThatThrownBy(() -> service.findById(otherCity.getId()))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value());
                    assertThat(exception.getReason()).isEqualTo("Questão não encontrada");
                });
    }

    @Test
    void deniesStudentBeforeQueryingQuestionData() {
        when(permissionService.getCurrentUser()).thenReturn(user(RoleEnum.ALUNO, "city-a"));

        assertThatThrownBy(() -> service.findAll(null, null, null))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode().value()).isEqualTo(HttpStatus.FORBIDDEN.value())
                );

        verify(questionRepository, never()).findAccessible(
                any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    private void mockReferences() {
        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(gradeRepository.findById(grade.getId())).thenReturn(Optional.of(grade));
        when(educationStageRepository.findById(stage.getId())).thenReturn(Optional.of(stage));
    }

    private QuestionUpsertRequest request() {
        return request(
                "multipleChoice",
                "A",
                List.of(
                        new QuestionOptionDTO("Z", "4", true, null),
                        new QuestionOptionDTO("Y", "5", false, null)
                ),
                1D,
                "Básico"
        );
    }

    private QuestionUpsertRequest request(
            String type,
            String solution,
            List<QuestionOptionDTO> options,
            Double value,
            String difficulty
    ) {
        return new QuestionUpsertRequest(
                "Adição",
                null,
                "Quanto é 2 + 2?",
                "<p>Quanto é 2 + 2?</p>",
                null,
                type,
                subject.getId(),
                stage.getId().toString(),
                grade.getId().toString(),
                difficulty,
                value,
                solution,
                null,
                options,
                List.of(),
                List.of()
        );
    }

    private User user(RoleEnum role, String cityId) {
        City city = new City();
        city.setId(cityId);
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setName("Usuário");
        user.setRole(role);
        user.setCity(city);
        return user;
    }

    private Question legacyQuestion() {
        Question question = new Question();
        question.setId(UUID.randomUUID().toString());
        question.setStatement("Questão legada");
        question.setCorrectOption("A");
        question.setScopeType(null);
        return question;
    }
}
