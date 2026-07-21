package com.gcoedu.core.service.tenant;

import com.gcoedu.core.domain.dto.tenant.QuestionDTO;
import com.gcoedu.core.domain.dto.tenant.QuestionOptionDTO;
import com.gcoedu.core.domain.dto.tenant.QuestionUpsertRequest;
import com.gcoedu.core.domain.entity.publics.Grade;
import com.gcoedu.core.domain.entity.publics.RoleEnum;
import com.gcoedu.core.domain.entity.publics.Subject;
import com.gcoedu.core.domain.entity.publics.User;
import com.gcoedu.core.repository.publics.GradeRepository;
import com.gcoedu.core.repository.publics.SubjectRepository;
import com.gcoedu.core.repository.publics.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class QuestionServiceIntegrationTest {

    @Autowired QuestionService questionService;
    @Autowired UserRepository userRepository;
    @Autowired SubjectRepository subjectRepository;
    @Autowired GradeRepository gradeRepository;

    private User tecadm;

    @BeforeEach
    void authenticateTecadmWithoutCredentials() {
        tecadm = userRepository.findAll().stream()
                .filter(user -> user.getRole() == RoleEnum.TECADM)
                .filter(user -> user.getCity() != null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "O ambiente de integração precisa de um TECADM vinculado a um município"
                ));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        tecadm.getEmail(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_TECADM"))
                )
        );
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void exposesLegacyRowsThroughTheRichScopedContract() {
        List<QuestionDTO> questions = questionService.findAll(null, null, null);

        assertThat(questions).isNotEmpty();
        assertThat(questions).allSatisfy(question -> {
            assertThat(question.id()).isNotBlank();
            assertThat(question.title()).isNotBlank();
            assertThat(question.text()).isNotBlank();
            assertThat(question.type()).isNotBlank();
            assertThat(question.difficulty()).isNotBlank();
            assertThat(question.value()).isNotNull();
            assertThat(question.scopeType()).isIn("GLOBAL", "CITY", "PRIVATE");
            assertThat(question.permissions()).isNotNull();
        });
    }

    @Test
    void appliesCreatedByFilterWithoutTrustingClientScopeFlags() {
        List<QuestionDTO> mine = questionService.findAll(tecadm.getId(), null, null);

        assertThat(mine).allSatisfy(question ->
                assertThat(question.createdBy()).isNotNull()
        );
        assertThat(mine).allSatisfy(question ->
                assertThat(question.createdBy().id()).isEqualTo(tecadm.getId())
        );
    }

    @Test
    void createsAndUpdatesCityQuestionInsideRolledBackTransaction() {
        Subject subject = subjectRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Cadastre uma disciplina para o teste"));
        Grade grade = gradeRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Cadastre uma série para o teste"));
        QuestionUpsertRequest createRequest = request(subject, grade, "Questão de integração");

        QuestionDTO created = questionService.create(createRequest);
        QuestionDTO loaded = questionService.findById(created.id());

        assertThat(loaded.title()).isEqualTo("Questão de integração");
        assertThat(loaded.scopeType()).isEqualTo("CITY");
        assertThat(loaded.createdBy().id()).isEqualTo(tecadm.getId());
        assertThat(loaded.permissions().canEdit()).isTrue();

        QuestionDTO updated = questionService.update(
                created.id(),
                request(subject, grade, "Questão de integração atualizada")
        );
        assertThat(updated.title()).isEqualTo("Questão de integração atualizada");
        assertThat(updated.version()).isEqualTo(2);
    }

    private QuestionUpsertRequest request(Subject subject, Grade grade, String title) {
        return new QuestionUpsertRequest(
                title,
                null,
                "Quanto é 2 + 2?",
                "<p>Quanto é 2 + 2?</p>",
                null,
                "multipleChoice",
                subject.getId(),
                grade.getEducationStage().getId().toString(),
                grade.getId().toString(),
                "Básico",
                1D,
                "A",
                null,
                List.of(
                        new QuestionOptionDTO("A", "4", true, null),
                        new QuestionOptionDTO("B", "5", false, null)
                ),
                List.of(),
                List.of()
        );
    }
}
