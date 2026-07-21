package com.gcoedu.core.service.publics;

import com.gcoedu.core.domain.dto.publics.SubjectDTO;
import com.gcoedu.core.domain.entity.publics.Subject;
import com.gcoedu.core.mapper.publics.SubjectMapper;
import com.gcoedu.core.repository.publics.SubjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubjectServiceTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private SubjectMapper subjectMapper;

    @InjectMocks
    private SubjectService subjectService;

    @Test
    void shouldCreateSubjectWithNormalizedName() {
        SubjectDTO request = new SubjectDTO();
        request.setName("  Educação   Física  ");

        SubjectDTO response = new SubjectDTO();
        response.setId("subject-id");
        response.setName("Educação Física");

        when(subjectRepository.findByNameIgnoreCase("Educação Física")).thenReturn(Optional.empty());
        when(subjectRepository.save(any(Subject.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(subjectMapper.toDto(any(Subject.class))).thenReturn(response);

        SubjectDTO result = subjectService.create(request);

        ArgumentCaptor<Subject> subjectCaptor = ArgumentCaptor.forClass(Subject.class);
        verify(subjectRepository).save(subjectCaptor.capture());

        assertThat(subjectCaptor.getValue().getId()).isNotBlank();
        assertThat(subjectCaptor.getValue().getName()).isEqualTo("Educação Física");
        assertThat(result).isSameAs(response);
    }

    @Test
    void shouldRejectDuplicateSubjectNameIgnoringCase() {
        SubjectDTO request = new SubjectDTO();
        request.setName("física");

        Subject existing = new Subject();
        existing.setId("existing-id");
        existing.setName("Física");

        when(subjectRepository.findByNameIgnoreCase("física")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> subjectService.create(request))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.CONFLICT.value());
                    assertThat(ex.getReason()).isEqualTo("Já existe uma disciplina com este nome");
                });
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingMissingSubject() {
        SubjectDTO request = new SubjectDTO();
        request.setName("Física");

        when(subjectRepository.findById("missing-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.update("missing-id", request))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value());
                    assertThat(ex.getReason()).isEqualTo("Disciplina não encontrada");
                });
    }

    @Test
    void shouldRejectDeletionWhenSubjectIsInUse() {
        Subject existing = new Subject();
        existing.setId("subject-id");
        existing.setName("Física");

        when(subjectRepository.findById("subject-id")).thenReturn(Optional.of(existing));
        doThrow(new DataIntegrityViolationException("foreign key"))
                .when(subjectRepository)
                .flush();

        assertThatThrownBy(() -> subjectService.delete("subject-id"))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.CONFLICT.value());
                    assertThat(ex.getReason())
                            .isEqualTo("Não é possível excluir esta disciplina porque ela está em uso.");
                });
    }
}
