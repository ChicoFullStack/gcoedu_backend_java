package com.gcoedu.core.service.tenant;

import com.gcoedu.core.config.tenant.TenantContext;
import com.gcoedu.core.domain.dto.tenant.PlayTvVideoDTO;
import com.gcoedu.core.domain.dto.tenant.playtv.CreatePlayTvVideoDTO;
import com.gcoedu.core.domain.dto.tenant.playtv.PlayTvLinkResourceDTO;
import com.gcoedu.core.domain.dto.tenant.playtv.UpdatePlayTvVideoDTO;
import com.gcoedu.core.domain.entity.publics.Grade;
import com.gcoedu.core.domain.entity.publics.RoleEnum;
import com.gcoedu.core.domain.entity.publics.Subject;
import com.gcoedu.core.domain.entity.publics.User;
import com.gcoedu.core.domain.entity.tenant.PlayTvVideo;
import com.gcoedu.core.domain.entity.tenant.PlayTvVideoClass;
import com.gcoedu.core.domain.entity.tenant.PlayTvVideoResource;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlayTvService {

    private static final long MAX_FILE_SIZE = 50L * 1024L * 1024L;
    private static final Set<RoleEnum> CREATOR_ROLES = Set.of(
            RoleEnum.ADMIN,
            RoleEnum.TECADM,
            RoleEnum.DIRETOR,
            RoleEnum.COORDENADOR,
            RoleEnum.PROFESSOR
    );

    private final PlayTvRepository playTvRepository;
    private final PlayTvMapper playTvMapper;
    private final SchoolRepository schoolRepository;
    private final ClassRepository classRepository;
    private final GradeRepository gradeRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final SchoolTeacherRepository schoolTeacherRepository;
    private final TeacherClassRepository teacherClassRepository;
    private final PermissionService permissionService;
    private final MinioService minioService;

    @Transactional(readOnly = true)
    public List<PlayTvVideoDTO> findVisible(String schoolId, String gradeId, String subjectId) {
        User user = requireCurrentUser();
        AudienceContext context = audienceContext(user);
        List<PlayTvVideo> videos = playTvRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(video -> canView(video, user, context))
                .filter(video -> matchesFilters(video, schoolId, gradeId, subjectId))
                .toList();
        return mapAll(videos);
    }

    @Transactional(readOnly = true)
    public PlayTvVideoDTO findVisibleById(String videoId) {
        User user = requireCurrentUser();
        PlayTvVideo video = findVideo(videoId);
        if (!canView(video, user, audienceContext(user))) {
            throw notFound();
        }
        return map(video);
    }

    @Transactional
    public PlayTvVideoDTO create(CreatePlayTvVideoDTO body) {
        User user = requireCreator();
        if (body == null) {
            throw badRequest("Dados do vídeo não informados");
        }

        PlayTvVideo video = new PlayTvVideo();
        video.setUrl(normalizeAndValidateUrl(body.getUrl(), "Informe uma URL válida para o vídeo"));
        video.setTitle(normalizeOptional(body.getTitle(), 100));
        Grade grade = requireGrade(body.getGrade());
        video.setGrade(grade);
        video.setSubject(requireSubject(body.getSubject()));
        video.setCreatedBy(user.getId());
        video.setCreatedAt(LocalDateTime.now());
        video.setUpdatedAt(LocalDateTime.now());

        applyAudience(
                video,
                Boolean.TRUE.equals(body.getEntireMunicipality()),
                body.getSchools(),
                body.getClasses(),
                grade,
                user
        );
        replaceLinkResources(video, body.getResources());
        return map(playTvRepository.saveAndFlush(video));
    }

    @Transactional
    public PlayTvVideoDTO update(String videoId, UpdatePlayTvVideoDTO body) {
        User user = requireCreator();
        PlayTvVideo video = requireEditable(videoId, user);
        if (body == null) {
            throw badRequest("Dados do vídeo não informados");
        }

        if (body.isTitlePresent()) {
            video.setTitle(normalizeOptional(body.getTitle(), 100));
        }
        if (body.getUrl() != null) {
            video.setUrl(normalizeAndValidateUrl(body.getUrl(), "Informe uma URL válida para o vídeo"));
        }
        Grade grade = body.getGrade() == null ? video.getGrade() : requireGrade(body.getGrade());
        if (body.getGrade() != null) {
            video.setGrade(grade);
        }
        if (body.getSubject() != null) {
            video.setSubject(requireSubject(body.getSubject()));
        }

        boolean audienceChanged = body.getEntireMunicipality() != null
                || body.getSchools() != null
                || body.getClasses() != null
                || body.getGrade() != null;
        if (audienceChanged) {
            applyAudience(
                    video,
                    body.getEntireMunicipality() == null
                            ? Boolean.TRUE.equals(video.getEntireMunicipality())
                            : body.getEntireMunicipality(),
                    body.getSchools() == null
                            ? new ArrayList<>(currentSchoolIds(video))
                            : body.getSchools(),
                    body.getClasses() == null
                            ? new ArrayList<>(currentClassIds(video))
                            : body.getClasses(),
                    grade,
                    user
            );
        }
        if (body.getResources() != null) {
            replaceLinkResources(video, body.getResources());
        }
        video.setUpdatedAt(LocalDateTime.now());
        return map(playTvRepository.saveAndFlush(video));
    }

    @Transactional
    public void delete(String videoId) {
        User user = requireCreator();
        PlayTvVideo video = requireDeletable(videoId, user);
        deleteStoredFiles(video.getResources());
        playTvRepository.delete(video);
    }

    @Transactional
    public PlayTvVideoDTO.PlayTvResourceDTO uploadFileResource(
            String videoId,
            MultipartFile file,
            String title,
            Integer sortOrder
    ) {
        User user = requireCreator();
        PlayTvVideo video = requireEditable(videoId, user);
        if (file == null || file.isEmpty()) {
            throw badRequest("Selecione um arquivo não vazio");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw badRequest("O arquivo deve ter no máximo 50 MB");
        }

        String resourceId = UUID.randomUUID().toString();
        String originalName = safeFileName(file.getOriginalFilename());
        String objectKey = "play-tv/" + safePathSegment(TenantContext.getCurrentTenant())
                + "/" + video.getId() + "/" + resourceId + "-" + originalName;
        try {
            minioService.uploadFile(
                    objectKey,
                    file.getInputStream(),
                    file.getSize(),
                    normalizeContentType(file.getContentType())
            );
        } catch (Exception exception) {
            throw unavailable("Não foi possível armazenar o anexo", exception);
        }

        PlayTvVideoResource resource = new PlayTvVideoResource();
        resource.setId(resourceId);
        resource.setVideo(video);
        resource.setResourceType("file");
        resource.setTitle(defaultTitle(title, originalName));
        resource.setOriginalFilename(originalName);
        resource.setContentType(normalizeContentType(file.getContentType()));
        resource.setSizeBytes(file.getSize());
        resource.setSortOrder(normalizeSortOrder(sortOrder, video.getResources().size()));
        resource.setMinioObjectName(objectKey);
        video.getResources().add(resource);
        video.setUpdatedAt(LocalDateTime.now());

        try {
            playTvRepository.saveAndFlush(video);
        } catch (RuntimeException exception) {
            try {
                minioService.deleteFile(objectKey);
            } catch (Exception ignored) {
                // A falha original de persistência é a informação mais útil ao cliente.
            }
            throw exception;
        }
        return mapResource(resource);
    }

    @Transactional(readOnly = true)
    public FileDownload downloadFileResource(String videoId, String resourceId) {
        User user = requireCurrentUser();
        PlayTvVideo video = findVideo(videoId);
        if (!canView(video, user, audienceContext(user))) {
            throw notFound();
        }
        PlayTvVideoResource resource = requireResource(video, resourceId, "file");
        if (trimToNull(resource.getMinioObjectName()) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Anexo não encontrado");
        }
        try (var input = minioService.downloadFile(resource.getMinioObjectName())) {
            return new FileDownload(
                    input.readAllBytes(),
                    normalizeContentType(resource.getContentType()),
                    safeFileName(resource.getOriginalFilename())
            );
        } catch (Exception exception) {
            throw unavailable("Não foi possível baixar o anexo", exception);
        }
    }

    @Transactional
    public void deleteResource(String videoId, String resourceId) {
        User user = requireCreator();
        PlayTvVideo video = requireEditable(videoId, user);
        PlayTvVideoResource resource = requireResource(video, resourceId, null);
        if (trimToNull(resource.getMinioObjectName()) != null) {
            try {
                minioService.deleteFile(resource.getMinioObjectName());
            } catch (Exception exception) {
                throw unavailable("Não foi possível remover o anexo", exception);
            }
        }
        video.getResources().remove(resource);
        video.setUpdatedAt(LocalDateTime.now());
        playTvRepository.saveAndFlush(video);
    }

    private void applyAudience(
            PlayTvVideo video,
            boolean entireMunicipality,
            List<String> requestedSchools,
            List<String> requestedClasses,
            Grade grade,
            User user
    ) {
        if (entireMunicipality) {
            if (user.getRole() != RoleEnum.ADMIN && user.getRole() != RoleEnum.TECADM) {
                throw forbidden("Seu perfil não pode publicar para todo o município");
            }
            video.setEntireMunicipality(true);
            video.getVideoSchools().clear();
            video.getVideoClasses().clear();
            return;
        }

        Set<String> schoolIds = normalizedIds(requestedSchools);
        if (schoolIds.isEmpty()) {
            throw badRequest("Selecione ao menos uma escola");
        }
        Set<String> allowedSchoolIds = allowedSchoolIds(user);
        if (!allowedSchoolIds.containsAll(schoolIds)) {
            throw forbidden("Uma ou mais escolas estão fora do seu escopo de acesso");
        }

        Map<String, School> schools = schoolRepository.findAllById(schoolIds).stream()
                .collect(Collectors.toMap(School::getId, Function.identity()));
        if (schools.size() != schoolIds.size()) {
            throw badRequest("Uma ou mais escolas não foram encontradas neste tenant");
        }

        Set<String> classIds = normalizedIds(requestedClasses);
        Map<String, SchoolClass> classes = classRepository.findAllById(classIds).stream()
                .collect(Collectors.toMap(SchoolClass::getId, Function.identity()));
        if (classes.size() != classIds.size()) {
            throw badRequest("Uma ou mais turmas não foram encontradas neste tenant");
        }
        for (SchoolClass schoolClass : classes.values()) {
            if (schoolClass.getSchool() == null
                    || !schoolIds.contains(schoolClass.getSchool().getId())) {
                throw badRequest("Todas as turmas devem pertencer às escolas selecionadas");
            }
            if (!Objects.equals(schoolClass.getGradeId(), grade.getId())) {
                throw badRequest("Todas as turmas devem pertencer à série selecionada");
            }
        }

        if (user.getRole() == RoleEnum.PROFESSOR) {
            if (classIds.isEmpty()) {
                throw badRequest("Professor deve selecionar ao menos uma turma vinculada");
            }
            Set<String> allowedClassIds = allowedClassIds(user);
            if (!allowedClassIds.containsAll(classIds)) {
                throw forbidden("Uma ou mais turmas não estão vinculadas ao professor");
            }
        } else if (!allowedClassIds(user).containsAll(classIds)) {
            throw forbidden("Uma ou mais turmas estão fora do seu escopo de acesso");
        }

        ensureGradeIsOfferedBySchools(schoolIds, grade.getId());
        video.setEntireMunicipality(false);
        video.getVideoSchools().clear();
        for (String schoolId : schoolIds) {
            PlayTvVideoSchool link = new PlayTvVideoSchool();
            link.setVideo(video);
            link.setSchool(schools.get(schoolId));
            video.getVideoSchools().add(link);
        }
        video.getVideoClasses().clear();
        for (String classId : classIds) {
            PlayTvVideoClass link = new PlayTvVideoClass();
            link.setVideo(video);
            link.setSchoolClass(classes.get(classId));
            video.getVideoClasses().add(link);
        }
    }

    private void ensureGradeIsOfferedBySchools(Set<String> schoolIds, UUID gradeId) {
        Set<String> schoolsWithGrade = classRepository.findAll().stream()
                .filter(item -> item.getSchool() != null)
                .filter(item -> schoolIds.contains(item.getSchool().getId()))
                .filter(item -> Objects.equals(item.getGradeId(), gradeId))
                .map(item -> item.getSchool().getId())
                .collect(Collectors.toSet());
        if (!schoolsWithGrade.containsAll(schoolIds)) {
            throw badRequest("A série selecionada não está disponível em todas as escolas");
        }
    }

    private void replaceLinkResources(
            PlayTvVideo video,
            List<PlayTvLinkResourceDTO> requestedResources
    ) {
        List<PlayTvLinkResourceDTO> requested = requestedResources == null
                ? List.of()
                : requestedResources;
        Map<String, PlayTvVideoResource> existingLinks = video.getResources().stream()
                .filter(resource -> "link".equalsIgnoreCase(resource.getResourceType()))
                .collect(Collectors.toMap(PlayTvVideoResource::getId, Function.identity()));
        Set<String> retainedIds = new HashSet<>();
        List<PlayTvVideoResource> additions = new ArrayList<>();
        int defaultOrder = 0;

        for (PlayTvLinkResourceDTO payload : requested) {
            if (payload == null || !"link".equalsIgnoreCase(trimToNull(payload.getType()))) {
                throw badRequest("Apenas links podem ser enviados no corpo do vídeo");
            }
            String title = normalizeRequired(payload.getTitle(), 200, "Informe o título de todos os links");
            String url = normalizeAndValidateUrl(payload.getUrl(), "Informe uma URL http/https válida para cada link");
            String requestedId = trimToNull(payload.getId());
            PlayTvVideoResource resource;
            if (requestedId == null) {
                resource = new PlayTvVideoResource();
                resource.setVideo(video);
                resource.setResourceType("link");
                additions.add(resource);
            } else {
                resource = existingLinks.get(requestedId);
                if (resource == null || !retainedIds.add(requestedId)) {
                    throw badRequest("Link complementar inválido ou duplicado");
                }
            }
            resource.setTitle(title);
            resource.setUrl(url);
            resource.setSortOrder(normalizeSortOrder(payload.getSortOrder(), defaultOrder));
            defaultOrder++;
        }

        video.getResources().removeIf(resource ->
                "link".equalsIgnoreCase(resource.getResourceType())
                        && !retainedIds.contains(resource.getId()));
        video.getResources().addAll(additions);
    }

    private boolean matchesFilters(
            PlayTvVideo video,
            String requestedSchoolId,
            String requestedGradeId,
            String requestedSubjectId
    ) {
        String schoolId = trimToNull(requestedSchoolId);
        String gradeId = trimToNull(requestedGradeId);
        String subjectId = trimToNull(requestedSubjectId);
        boolean schoolMatches = schoolId == null
                || Boolean.TRUE.equals(video.getEntireMunicipality())
                || currentSchoolIds(video).contains(schoolId);
        return schoolMatches
                && (gradeId == null || Objects.equals(gradeId, video.getGrade().getId().toString()))
                && (subjectId == null || Objects.equals(subjectId, video.getSubject().getId()));
    }

    private boolean canView(PlayTvVideo video, User user, AudienceContext context) {
        if (Objects.equals(video.getCreatedBy(), user.getId())) {
            return true;
        }
        if (user.getRole() == RoleEnum.ADMIN || user.getRole() == RoleEnum.TECADM) {
            return true;
        }
        if (user.getRole() == RoleEnum.APLICADOR) {
            return false;
        }

        if (Boolean.TRUE.equals(video.getEntireMunicipality())) {
            return user.getRole() != RoleEnum.ALUNO
                    || Objects.equals(video.getGrade().getId().toString(), context.gradeId());
        }

        Set<String> targetSchools = currentSchoolIds(video);
        Set<String> targetClasses = currentClassIds(video);
        if (user.getRole() == RoleEnum.DIRETOR || user.getRole() == RoleEnum.COORDENADOR) {
            return intersects(targetSchools, context.schoolIds());
        }
        if (user.getRole() == RoleEnum.PROFESSOR) {
            return intersects(targetSchools, context.schoolIds())
                    && (targetClasses.isEmpty() || intersects(targetClasses, context.classIds()));
        }
        if (user.getRole() == RoleEnum.ALUNO) {
            return Objects.equals(video.getGrade().getId().toString(), context.gradeId())
                    && intersects(targetSchools, context.schoolIds())
                    && (targetClasses.isEmpty() || targetClasses.contains(context.studentClassId()));
        }
        return false;
    }

    private PlayTvVideo requireEditable(String videoId, User user) {
        PlayTvVideo video = findVideo(videoId);
        if (Objects.equals(video.getCreatedBy(), user.getId())
                || user.getRole() == RoleEnum.ADMIN
                || user.getRole() == RoleEnum.TECADM) {
            return video;
        }
        if ((user.getRole() == RoleEnum.DIRETOR || user.getRole() == RoleEnum.COORDENADOR)
                && intersects(currentSchoolIds(video), allowedSchoolIds(user))) {
            return video;
        }
        throw forbidden("Seu perfil não pode alterar este vídeo");
    }

    private PlayTvVideo requireDeletable(String videoId, User user) {
        PlayTvVideo video = findVideo(videoId);
        if (Objects.equals(video.getCreatedBy(), user.getId())
                || user.getRole() == RoleEnum.ADMIN
                || user.getRole() == RoleEnum.TECADM) {
            return video;
        }
        if (user.getRole() == RoleEnum.DIRETOR
                && intersects(currentSchoolIds(video), allowedSchoolIds(user))) {
            return video;
        }
        throw forbidden("Seu perfil não pode excluir este vídeo");
    }

    private AudienceContext audienceContext(User user) {
        Set<String> schoolIds = new HashSet<>();
        Set<String> classIds = new HashSet<>();
        String gradeId = null;
        String studentClassId = null;

        if (user.getRole() == RoleEnum.DIRETOR || user.getRole() == RoleEnum.COORDENADOR) {
            String managedSchoolId = permissionService.getCurrentManagedSchoolId();
            if (managedSchoolId != null) schoolIds.add(managedSchoolId);
        } else if (user.getRole() == RoleEnum.PROFESSOR) {
            schoolIds.addAll(schoolTeacherRepository.findSchoolIdsByTeacherUserId(user.getId()));
            classIds.addAll(teacherClassRepository.findClassIdsByTeacherUserId(user.getId()));
            if (!classIds.isEmpty()) {
                classRepository.findAllById(classIds).stream()
                        .filter(item -> item.getSchool() != null)
                        .map(item -> item.getSchool().getId())
                        .forEach(schoolIds::add);
            }
        } else if (user.getRole() == RoleEnum.ALUNO) {
            Student student = studentRepository.findByUserId(user.getId()).orElse(null);
            if (student != null) {
                if (student.getSchool() != null) schoolIds.add(student.getSchool().getId());
                if (student.getGradeId() != null) gradeId = student.getGradeId().toString();
                if (student.getSchoolClass() != null) {
                    studentClassId = student.getSchoolClass().getId();
                    classIds.add(studentClassId);
                }
            }
        }
        return new AudienceContext(schoolIds, classIds, gradeId, studentClassId);
    }

    private Set<String> allowedSchoolIds(User user) {
        if (user.getRole() == RoleEnum.ADMIN || user.getRole() == RoleEnum.TECADM) {
            return schoolRepository.findAll().stream().map(School::getId).collect(Collectors.toSet());
        }
        if (user.getRole() == RoleEnum.DIRETOR || user.getRole() == RoleEnum.COORDENADOR) {
            String schoolId = permissionService.getCurrentManagedSchoolId();
            return schoolId == null ? Set.of() : Set.of(schoolId);
        }
        if (user.getRole() == RoleEnum.PROFESSOR) {
            Set<String> result = new HashSet<>(
                    schoolTeacherRepository.findSchoolIdsByTeacherUserId(user.getId())
            );
            Set<String> classIds = new HashSet<>(
                    teacherClassRepository.findClassIdsByTeacherUserId(user.getId())
            );
            classRepository.findAllById(classIds).stream()
                    .filter(item -> item.getSchool() != null)
                    .map(item -> item.getSchool().getId())
                    .forEach(result::add);
            return result;
        }
        return Set.of();
    }

    private Set<String> allowedClassIds(User user) {
        if (user.getRole() == RoleEnum.ADMIN || user.getRole() == RoleEnum.TECADM) {
            return classRepository.findAll().stream().map(SchoolClass::getId).collect(Collectors.toSet());
        }
        if (user.getRole() == RoleEnum.DIRETOR || user.getRole() == RoleEnum.COORDENADOR) {
            Set<String> schools = allowedSchoolIds(user);
            return classRepository.findAll().stream()
                    .filter(item -> item.getSchool() != null)
                    .filter(item -> schools.contains(item.getSchool().getId()))
                    .map(SchoolClass::getId)
                    .collect(Collectors.toSet());
        }
        if (user.getRole() == RoleEnum.PROFESSOR) {
            return new HashSet<>(teacherClassRepository.findClassIdsByTeacherUserId(user.getId()));
        }
        return Set.of();
    }

    private List<PlayTvVideoDTO> mapAll(List<PlayTvVideo> videos) {
        Map<String, User> creators = userRepository.findAllById(
                        videos.stream()
                                .map(PlayTvVideo::getCreatedBy)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet())
                ).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return videos.stream()
                .map(video -> playTvMapper.toDto(video, creators.get(video.getCreatedBy())))
                .toList();
    }

    private PlayTvVideoDTO map(PlayTvVideo video) {
        User creator = trimToNull(video.getCreatedBy()) == null
                ? null
                : userRepository.findById(video.getCreatedBy()).orElse(null);
        return playTvMapper.toDto(video, creator);
    }

    private PlayTvVideoDTO.PlayTvResourceDTO mapResource(PlayTvVideoResource resource) {
        PlayTvVideoDTO.PlayTvResourceDTO dto = new PlayTvVideoDTO.PlayTvResourceDTO();
        dto.setId(resource.getId());
        dto.setType(resource.getResourceType());
        dto.setTitle(resource.getTitle());
        dto.setUrl(resource.getUrl());
        dto.setFileName(resource.getOriginalFilename());
        dto.setMimeType(resource.getContentType());
        dto.setSizeBytes(resource.getSizeBytes());
        dto.setSortOrder(resource.getSortOrder());
        return dto;
    }

    private Grade requireGrade(String rawId) {
        try {
            UUID id = UUID.fromString(normalizeRequired(rawId, 36, "Informe uma série válida"));
            return gradeRepository.findById(id)
                    .orElseThrow(() -> badRequest("Série não encontrada"));
        } catch (IllegalArgumentException exception) {
            throw badRequest("Informe uma série válida");
        }
    }

    private Subject requireSubject(String rawId) {
        String id = normalizeRequired(rawId, 64, "Informe uma disciplina válida");
        return subjectRepository.findById(id)
                .orElseThrow(() -> badRequest("Disciplina não encontrada"));
    }

    private PlayTvVideo findVideo(String rawId) {
        String id = trimToNull(rawId);
        if (id == null) throw notFound();
        return playTvRepository.findById(id).orElseThrow(this::notFound);
    }

    private PlayTvVideoResource requireResource(
            PlayTvVideo video,
            String rawResourceId,
            String expectedType
    ) {
        String resourceId = trimToNull(rawResourceId);
        return video.getResources().stream()
                .filter(resource -> Objects.equals(resourceId, resource.getId()))
                .filter(resource -> expectedType == null
                        || expectedType.equalsIgnoreCase(resource.getResourceType()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Recurso não encontrado"
                ));
    }

    private User requireCurrentUser() {
        User user = permissionService.getCurrentUser();
        if (user == null || user.getRole() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuário autenticado não encontrado"
            );
        }
        return user;
    }

    private User requireCreator() {
        User user = requireCurrentUser();
        if (!CREATOR_ROLES.contains(user.getRole())) {
            throw forbidden("Seu perfil não pode gerenciar vídeos do Play TV");
        }
        return user;
    }

    private void deleteStoredFiles(List<PlayTvVideoResource> resources) {
        for (PlayTvVideoResource resource : resources) {
            if (trimToNull(resource.getMinioObjectName()) == null) continue;
            try {
                minioService.deleteFile(resource.getMinioObjectName());
            } catch (Exception exception) {
                throw unavailable("Não foi possível remover os anexos do vídeo", exception);
            }
        }
    }

    private Set<String> currentSchoolIds(PlayTvVideo video) {
        if (video.getVideoSchools() == null) return Set.of();
        return video.getVideoSchools().stream()
                .map(PlayTvVideoSchool::getSchool)
                .filter(Objects::nonNull)
                .map(School::getId)
                .collect(Collectors.toSet());
    }

    private Set<String> currentClassIds(PlayTvVideo video) {
        if (video.getVideoClasses() == null) return Set.of();
        return video.getVideoClasses().stream()
                .map(PlayTvVideoClass::getSchoolClass)
                .filter(Objects::nonNull)
                .map(SchoolClass::getId)
                .collect(Collectors.toSet());
    }

    private Set<String> normalizedIds(List<String> values) {
        if (values == null) return Set.of();
        return values.stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean intersects(Set<String> left, Set<String> right) {
        return left.stream().anyMatch(right::contains);
    }

    private String normalizeAndValidateUrl(String value, String message) {
        String url = normalizeRequired(value, 2000, message);
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (uri.getHost() == null
                    || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw badRequest(message);
            }
            return url;
        } catch (IllegalArgumentException exception) {
            throw badRequest(message);
        }
    }

    private String normalizeRequired(String value, int max, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) throw badRequest(message);
        if (normalized.length() > max) throw badRequest("O valor informado excede o limite permitido");
        return normalized;
    }

    private String normalizeOptional(String value, int max) {
        String normalized = trimToNull(value);
        if (normalized == null) return null;
        if (normalized.length() > max) throw badRequest("O valor informado excede o limite permitido");
        return normalized;
    }

    private String defaultTitle(String value, String fallback) {
        String normalized = normalizeOptional(value, 200);
        return normalized == null ? fallback : normalized;
    }

    private Integer normalizeSortOrder(Integer value, int fallback) {
        if (value == null) return fallback;
        if (value < 0 || value > 10000) {
            throw badRequest("Ordem do recurso inválida");
        }
        return value;
    }

    private String normalizeContentType(String value) {
        String normalized = trimToNull(value);
        if (normalized == null || normalized.length() > 200) {
            return "application/octet-stream";
        }
        return normalized;
    }

    private String safeFileName(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) return "arquivo";
        normalized = normalized.replace("\\", "/");
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1);
        normalized = normalized.replaceAll("[^\\p{L}\\p{N}._ -]", "_");
        return normalized.length() > 180
                ? normalized.substring(normalized.length() - 180)
                : normalized;
    }

    private String safePathSegment(String value) {
        String normalized = trimToNull(value);
        return normalized == null
                ? "tenant"
                : normalized.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Vídeo não encontrado");
    }

    private ResponseStatusException unavailable(String message, Exception cause) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, message, cause);
    }

    public record FileDownload(byte[] content, String contentType, String fileName) {}

    private record AudienceContext(
            Set<String> schoolIds,
            Set<String> classIds,
            String gradeId,
            String studentClassId
    ) {}
}
