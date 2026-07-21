package com.gcoedu.core.service.tenant;

import com.gcoedu.core.domain.dto.tenant.playtv.CreatePlayTvVideoDTO;
import com.gcoedu.core.domain.dto.tenant.playtv.PlayTvLinkResourceDTO;
import com.gcoedu.core.domain.dto.tenant.playtv.UpdatePlayTvVideoDTO;
import com.gcoedu.core.domain.entity.publics.Grade;
import com.gcoedu.core.domain.entity.publics.RoleEnum;
import com.gcoedu.core.domain.entity.publics.Subject;
import com.gcoedu.core.domain.entity.publics.User;
import com.gcoedu.core.domain.entity.tenant.PlayTvVideo;
import com.gcoedu.core.domain.entity.tenant.PlayTvVideoClass;
import com.gcoedu.core.domain.entity.tenant.PlayTvVideoSchool;
import com.gcoedu.core.domain.entity.tenant.School;
import com.gcoedu.core.domain.entity.tenant.SchoolClass;
import com.gcoedu.core.domain.entity.tenant.Student;
import com.gcoedu.core.mapper.tenant.PlayTvMapper;
import com.gcoedu.core.repository.publics.GradeRepository;
import com.gcoedu.core.repository.publics.SubjectRepository;
import com.gcoedu.core.repository.publics.UserRepository;
import com.gcoedu.core.repository.tenant.ClassRepository;
import com.gcoedu.core.repository.tenant.PlayTvRepository;
import com.gcoedu.core.repository.tenant.SchoolRepository;
import com.gcoedu.core.repository.tenant.SchoolTeacherRepository;
import com.gcoedu.core.repository.tenant.StudentRepository;
import com.gcoedu.core.repository.tenant.TeacherClassRepository;
import com.gcoedu.core.service.MinioService;
import com.gcoedu.core.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class PlayTvServiceTest {

    @Mock PlayTvRepository playTvRepository;
    @Mock SchoolRepository schoolRepository;
    @Mock ClassRepository classRepository;
    @Mock GradeRepository gradeRepository;
    @Mock SubjectRepository subjectRepository;
    @Mock UserRepository userRepository;
    @Mock StudentRepository studentRepository;
    @Mock SchoolTeacherRepository schoolTeacherRepository;
    @Mock TeacherClassRepository teacherClassRepository;
    @Mock PermissionService permissionService;
    @Mock MinioService minioService;

    private PlayTvService service;

    @BeforeEach
    void setUp() {
        service = new PlayTvService(
                playTvRepository,
                new PlayTvMapper(),
                schoolRepository,
                classRepository,
                gradeRepository,
                subjectRepository,
                userRepository,
                studentRepository,
                schoolTeacherRepository,
                teacherClassRepository,
                permissionService,
                minioService
        );
    }

    @Test
    void createsScopedVideoUsingAuthenticatedOwner() {
        User admin = user(RoleEnum.ADMIN);
        Grade grade = grade();
        Subject subject = subject();
        School school = school();
        SchoolClass schoolClass = schoolClass(school, grade);
        CreatePlayTvVideoDTO body = request(grade, subject, school, schoolClass);

        when(permissionService.getCurrentUser()).thenReturn(admin);
        when(gradeRepository.findById(grade.getId())).thenReturn(Optional.of(grade));
        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(schoolRepository.findAll()).thenReturn(List.of(school));
        when(schoolRepository.findAllById(any())).thenReturn(List.of(school));
        when(classRepository.findAllById(any())).thenReturn(List.of(schoolClass));
        when(classRepository.findAll()).thenReturn(List.of(schoolClass));
        when(playTvRepository.saveAndFlush(any(PlayTvVideo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        var result = service.create(body);

        assertThat(result.getCreatedBy().getId()).isEqualTo(admin.getId());
        assertThat(result.getSchools()).extracting(item -> item.getId())
                .containsExactly(school.getId());
        assertThat(result.getClasses()).extracting(item -> item.getId())
                .containsExactly(schoolClass.getId());
        assertThat(result.getResources()).hasSize(1);
        verify(playTvRepository).saveAndFlush(any(PlayTvVideo.class));
    }

    @Test
    void rejectsStudentCreationBeforePersistence() {
        when(permissionService.getCurrentUser()).thenReturn(user(RoleEnum.ALUNO));

        assertThatThrownBy(() -> service.create(new CreatePlayTvVideoDTO()))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode().value())
                                .isEqualTo(HttpStatus.FORBIDDEN.value()));

        verify(playTvRepository, never()).saveAndFlush(any(PlayTvVideo.class));
    }

    @Test
    void rejectsMunicipalityWidePublicationForProfessor() {
        User professor = user(RoleEnum.PROFESSOR);
        Grade grade = grade();
        Subject subject = subject();
        CreatePlayTvVideoDTO body = new CreatePlayTvVideoDTO();
        body.setUrl("https://www.youtube.com/watch?v=abc");
        body.setGrade(grade.getId().toString());
        body.setSubject(subject.getId());
        body.setEntireMunicipality(true);

        when(permissionService.getCurrentUser()).thenReturn(professor);
        when(gradeRepository.findById(grade.getId())).thenReturn(Optional.of(grade));
        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));

        assertThatThrownBy(() -> service.create(body))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode().value())
                                .isEqualTo(HttpStatus.FORBIDDEN.value()));
    }

    @Test
    void studentOnlySeesVideosForOwnGradeSchoolAndClass() {
        User studentUser = user(RoleEnum.ALUNO);
        Grade grade = grade();
        Subject subject = subject();
        School ownSchool = school();
        School otherSchool = school();
        SchoolClass ownClass = schoolClass(ownSchool, grade);
        Student student = new Student();
        student.setUser(studentUser);
        student.setSchool(ownSchool);
        student.setGradeId(grade.getId());
        student.setSchoolClass(ownClass);

        PlayTvVideo visible = video(grade, subject, ownSchool, ownClass, "creator-a");
        PlayTvVideo wrongSchool = video(grade, subject, otherSchool, null, "creator-b");
        PlayTvVideo wrongClass = video(grade, subject, ownSchool, schoolClass(ownSchool, grade), "creator-c");

        when(permissionService.getCurrentUser()).thenReturn(studentUser);
        when(studentRepository.findByUserId(studentUser.getId())).thenReturn(Optional.of(student));
        when(playTvRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(visible, wrongSchool, wrongClass));
        when(userRepository.findAllById(any())).thenReturn(List.of());

        var result = service.findVisible(null, null, null);

        assertThat(result).extracting(item -> item.getId()).containsExactly(visible.getId());
    }

    @Test
    void hidesVideoOutsideStudentAudienceAsNotFound() {
        User studentUser = user(RoleEnum.ALUNO);
        Grade grade = grade();
        Subject subject = subject();
        School ownSchool = school();
        School otherSchool = school();
        Student student = new Student();
        student.setUser(studentUser);
        student.setSchool(ownSchool);
        student.setGradeId(grade.getId());
        PlayTvVideo hidden = video(grade, subject, otherSchool, null, "creator");

        when(permissionService.getCurrentUser()).thenReturn(studentUser);
        when(studentRepository.findByUserId(studentUser.getId())).thenReturn(Optional.of(student));
        when(playTvRepository.findById(hidden.getId())).thenReturn(Optional.of(hidden));

        assertThatThrownBy(() -> service.findVisibleById(hidden.getId()))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode().value())
                                .isEqualTo(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void professorCannotEditAnotherCreatorsVideo() {
        User professor = user(RoleEnum.PROFESSOR);
        Grade grade = grade();
        Subject subject = subject();
        School school = school();
        PlayTvVideo foreign = video(grade, subject, school, null, "another-user");
        UpdatePlayTvVideoDTO body = new UpdatePlayTvVideoDTO();
        body.setTitle("Novo título");

        when(permissionService.getCurrentUser()).thenReturn(professor);
        when(playTvRepository.findById(foreign.getId())).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.update(foreign.getId(), body))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode().value())
                                .isEqualTo(HttpStatus.FORBIDDEN.value()));
    }

    @Test
    void rejectsHtmlIframeInsteadOfAcceptingMarkupAsVideoUrl() {
        User admin = user(RoleEnum.ADMIN);
        CreatePlayTvVideoDTO body = new CreatePlayTvVideoDTO();
        body.setUrl("<iframe src=\"https://example.com/video\"></iframe>");

        when(permissionService.getCurrentUser()).thenReturn(admin);

        assertThatThrownBy(() -> service.create(body))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode().value())
                                .isEqualTo(HttpStatus.BAD_REQUEST.value()));
    }

    private CreatePlayTvVideoDTO request(
            Grade grade,
            Subject subject,
            School school,
            SchoolClass schoolClass
    ) {
        PlayTvLinkResourceDTO link = new PlayTvLinkResourceDTO();
        link.setType("link");
        link.setTitle("Exercícios");
        link.setUrl("https://example.com/exercicios");

        CreatePlayTvVideoDTO body = new CreatePlayTvVideoDTO();
        body.setUrl("https://www.youtube.com/watch?v=abc");
        body.setTitle("Aula");
        body.setGrade(grade.getId().toString());
        body.setSubject(subject.getId());
        body.setSchools(List.of(school.getId()));
        body.setClasses(List.of(schoolClass.getId()));
        body.setResources(List.of(link));
        return body;
    }

    private PlayTvVideo video(
            Grade grade,
            Subject subject,
            School school,
            SchoolClass schoolClass,
            String creatorId
    ) {
        PlayTvVideo video = new PlayTvVideo();
        video.setUrl("https://www.youtube.com/watch?v=abc");
        video.setTitle("Aula");
        video.setGrade(grade);
        video.setSubject(subject);
        video.setCreatedBy(creatorId);
        PlayTvVideoSchool schoolLink = new PlayTvVideoSchool();
        schoolLink.setVideo(video);
        schoolLink.setSchool(school);
        video.getVideoSchools().add(schoolLink);
        if (schoolClass != null) {
            PlayTvVideoClass classLink = new PlayTvVideoClass();
            classLink.setVideo(video);
            classLink.setSchoolClass(schoolClass);
            video.getVideoClasses().add(classLink);
        }
        return video;
    }

    private User user(RoleEnum role) {
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setName("Usuário");
        user.setRole(role);
        return user;
    }

    private Grade grade() {
        Grade grade = new Grade();
        grade.setId(UUID.randomUUID());
        grade.setName("8º Ano");
        return grade;
    }

    private Subject subject() {
        Subject subject = new Subject();
        subject.setId(UUID.randomUUID().toString());
        subject.setName("Física");
        return subject;
    }

    private School school() {
        School school = new School();
        school.setId(UUID.randomUUID().toString());
        school.setName("Escola");
        return school;
    }

    private SchoolClass schoolClass(School school, Grade grade) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(UUID.randomUUID().toString());
        schoolClass.setName("Turma A");
        schoolClass.setSchool(school);
        schoolClass.setGradeId(grade.getId());
        return schoolClass;
    }
}
