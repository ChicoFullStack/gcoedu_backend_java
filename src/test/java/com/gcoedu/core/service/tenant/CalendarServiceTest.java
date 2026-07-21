package com.gcoedu.core.service.tenant;

import com.gcoedu.core.domain.dto.tenant.calendar.CreateEventBodyDTO;
import com.gcoedu.core.domain.entity.publics.City;
import com.gcoedu.core.domain.entity.publics.RoleEnum;
import com.gcoedu.core.domain.entity.publics.User;
import com.gcoedu.core.domain.entity.tenant.CalendarEvent;
import com.gcoedu.core.domain.entity.tenant.CalendarEventTarget;
import com.gcoedu.core.repository.publics.GradeRepository;
import com.gcoedu.core.repository.tenant.CalendarEventRepository;
import com.gcoedu.core.repository.tenant.ClassRepository;
import com.gcoedu.core.repository.tenant.SchoolRepository;
import com.gcoedu.core.repository.tenant.SchoolTeacherRepository;
import com.gcoedu.core.repository.tenant.StudentRepository;
import com.gcoedu.core.service.MinioService;
import com.gcoedu.core.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @Mock CalendarEventRepository calendarEventRepository;
    @Mock SchoolRepository schoolRepository;
    @Mock ClassRepository classRepository;
    @Mock StudentRepository studentRepository;
    @Mock SchoolTeacherRepository schoolTeacherRepository;
    @Mock GradeRepository gradeRepository;
    @Mock PermissionService permissionService;
    @Mock MinioService minioService;

    private CalendarService service;

    @BeforeEach
    void setUp() {
        service = new CalendarService(
                calendarEventRepository,
                schoolRepository,
                classRepository,
                studentRepository,
                schoolTeacherRepository,
                gradeRepository,
                permissionService,
                minioService
        );
    }

    @Test
    void createsPersonalEventUsingAuthenticatedOwnerAndCompleteDates() {
        User professor = user(RoleEnum.PROFESSOR, "city-a");
        when(permissionService.getCurrentUser()).thenReturn(professor);
        when(schoolRepository.findAllById(any())).thenReturn(List.of());
        when(classRepository.findAll()).thenReturn(List.of());
        when(calendarEventRepository.saveAndFlush(any(CalendarEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.createEvent(request(
                target("USER", professor.getId()),
                "2026-07-21T09:00:00-03:00",
                "2026-07-21T10:30:00-03:00"
        ));

        ArgumentCaptor<CalendarEvent> captor = ArgumentCaptor.forClass(CalendarEvent.class);
        verify(calendarEventRepository).saveAndFlush(captor.capture());
        CalendarEvent saved = captor.getValue();
        assertThat(saved.getCreatedByUserId()).isEqualTo(professor.getId());
        assertThat(saved.getCreatedByRole()).isEqualTo("professor");
        assertThat(saved.getStartAt())
                .isEqualTo(OffsetDateTime.parse("2026-07-21T09:00:00-03:00"));
        assertThat(saved.getEndAt())
                .isEqualTo(OffsetDateTime.parse("2026-07-21T10:30:00-03:00"));
        assertThat(saved.getVisibilityScope()).isEqualTo("USERS");
        assertThat(saved.isPublished()).isTrue();
        assertThat(result.getCreated_by().getId()).isEqualTo(professor.getId());
        assertThat(result.getExtendedProps()).containsEntry("location", "Sala 10");
    }

    @Test
    void rejectsStudentCreationBeforePersistence() {
        when(permissionService.getCurrentUser()).thenReturn(user(RoleEnum.ALUNO, "city-a"));

        assertThatThrownBy(() -> service.createEvent(request(
                target("USER", "student-id"),
                "2026-07-21T09:00:00-03:00",
                "2026-07-21T10:00:00-03:00"
        )))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode().value())
                                .isEqualTo(HttpStatus.FORBIDDEN.value()));

        verify(calendarEventRepository, never()).saveAndFlush(any(CalendarEvent.class));
    }

    @Test
    void rejectsMunicipalityWideTargetForProfessor() {
        User professor = user(RoleEnum.PROFESSOR, "city-a");
        when(permissionService.getCurrentUser()).thenReturn(professor);
        when(schoolRepository.findAllById(any())).thenReturn(List.of());
        when(classRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> service.createEvent(request(
                target("ALL", null),
                "2026-07-21T09:00:00-03:00",
                "2026-07-21T10:00:00-03:00"
        )))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode().value())
                            .isEqualTo(HttpStatus.FORBIDDEN.value());
                    assertThat(exception.getReason())
                            .isEqualTo("Seu perfil não pode enviar eventos para todo o município");
                });

        verify(calendarEventRepository, never()).saveAndFlush(any(CalendarEvent.class));
    }

    @Test
    void rejectsInvalidDateRangeBeforePersistence() {
        User admin = user(RoleEnum.ADMIN, "city-a");
        when(permissionService.getCurrentUser()).thenReturn(admin);

        assertThatThrownBy(() -> service.createEvent(request(
                target("USER", admin.getId()),
                "2026-07-21T11:00:00-03:00",
                "2026-07-21T10:00:00-03:00"
        )))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode().value())
                            .isEqualTo(HttpStatus.BAD_REQUEST.value());
                    assertThat(exception.getReason())
                            .isEqualTo("A data final deve ser posterior à data inicial");
                });

        verify(calendarEventRepository, never()).saveAndFlush(any(CalendarEvent.class));
    }

    @Test
    void listsOnlyEventsAddressedToCurrentUser() {
        User professor = user(RoleEnum.PROFESSOR, "city-a");
        CalendarEvent visible = eventForUser(professor.getId());
        CalendarEvent privateToAnotherUser = eventForUser("another-user");
        when(permissionService.getCurrentUser()).thenReturn(professor);
        when(schoolTeacherRepository.findSchoolIdsByTeacherUserId(professor.getId()))
                .thenReturn(List.of());
        when(calendarEventRepository
                .findByStartAtLessThanAndEndAtGreaterThanOrderByStartAtAsc(any(), any()))
                .thenReturn(List.of(visible, privateToAnotherUser));

        var result = service.listMyEvents(
                "2026-07-01T00:00:00-03:00",
                "2026-08-01T00:00:00-03:00",
                null,
                "aviso"
        );

        assertThat(result).extracting(item -> item.getId()).containsExactly(visible.getId());
    }

    @Test
    void hidesForeignEventAsNotFound() {
        User admin = user(RoleEnum.ADMIN, "city-a");
        CalendarEvent foreign = eventForUser("another-user");
        when(permissionService.getCurrentUser()).thenReturn(admin);
        when(calendarEventRepository.findById(foreign.getId())).thenReturn(java.util.Optional.of(foreign));

        assertThatThrownBy(() -> service.getEvent(foreign.getId()))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode().value())
                                .isEqualTo(HttpStatus.NOT_FOUND.value()));
    }

    private CreateEventBodyDTO request(
            CreateEventBodyDTO.CalendarTargetPayload target,
            String start,
            String end
    ) {
        CreateEventBodyDTO body = new CreateEventBodyDTO();
        body.setTitle(" Reunião pedagógica ");
        body.setDescription("Planejamento");
        body.setLocation(" Sala 10 ");
        body.setStart_at(start);
        body.setEnd_at(end);
        body.setTimezone("America/Sao_Paulo");
        body.setTargets(List.of(target));
        body.setIs_published(true);
        return body;
    }

    private CreateEventBodyDTO.CalendarTargetPayload target(String type, String id) {
        CreateEventBodyDTO.CalendarTargetPayload target =
                new CreateEventBodyDTO.CalendarTargetPayload();
        target.setTarget_type(type);
        target.setTarget_id(id);
        return target;
    }

    private CalendarEvent eventForUser(String userId) {
        CalendarEvent event = new CalendarEvent();
        event.setTitle("Evento");
        event.setStartAt(OffsetDateTime.parse("2026-07-21T09:00:00-03:00"));
        event.setEndAt(OffsetDateTime.parse("2026-07-21T10:00:00-03:00"));
        event.setTimezone("America/Sao_Paulo");
        event.setPublished(true);
        CalendarEventTarget target = new CalendarEventTarget();
        target.setEvent(event);
        target.setTargetType("USER");
        target.setTargetId(userId);
        event.setTargets(List.of(target));
        return event;
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
}
