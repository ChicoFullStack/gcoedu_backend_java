package com.gcoedu.core.service.tenant;

import com.gcoedu.core.config.tenant.TenantContext;
import com.gcoedu.core.domain.dto.tenant.calendar.CalendarEventDTO;
import com.gcoedu.core.domain.dto.tenant.calendar.CreateEventBodyDTO;
import com.gcoedu.core.domain.entity.publics.RoleEnum;
import com.gcoedu.core.domain.entity.publics.User;
import com.gcoedu.core.domain.entity.tenant.CalendarEvent;
import com.gcoedu.core.domain.entity.tenant.CalendarEventResource;
import com.gcoedu.core.domain.entity.tenant.CalendarEventTarget;
import com.gcoedu.core.domain.entity.tenant.School;
import com.gcoedu.core.domain.entity.tenant.SchoolClass;
import com.gcoedu.core.domain.entity.tenant.Student;
import com.gcoedu.core.repository.publics.GradeRepository;
import com.gcoedu.core.repository.tenant.CalendarEventRepository;
import com.gcoedu.core.repository.tenant.ClassRepository;
import com.gcoedu.core.repository.tenant.SchoolRepository;
import com.gcoedu.core.repository.tenant.SchoolTeacherRepository;
import com.gcoedu.core.repository.tenant.StudentRepository;
import com.gcoedu.core.service.MinioService;
import com.gcoedu.core.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;
    private static final Set<RoleEnum> EVENT_CREATORS = Set.of(
            RoleEnum.ADMIN,
            RoleEnum.TECADM,
            RoleEnum.DIRETOR,
            RoleEnum.COORDENADOR,
            RoleEnum.PROFESSOR
    );
    private static final Set<String> TARGET_TYPES = Set.of(
            "ALL", "MUNICIPALITY", "SCHOOL", "GRADE", "CLASS", "USER", "ROLE_GROUP"
    );
    private static final Set<String> ROLE_GROUPS = Set.of(
            "admin", "tecadm", "diretor", "coordenador", "professor", "aluno"
    );

    private final CalendarEventRepository calendarEventRepository;
    private final SchoolRepository schoolRepository;
    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final SchoolTeacherRepository schoolTeacherRepository;
    private final GradeRepository gradeRepository;
    private final PermissionService permissionService;
    private final MinioService minioService;

    @Transactional(readOnly = true)
    public List<CalendarEventDTO> listMyEvents(
            String start,
            String end,
            String kind,
            String excludeKind
    ) {
        User user = requireCurrentUser();
        OffsetDateTime rangeStart = parseOptionalDate(start, "Data inicial inválida");
        OffsetDateTime rangeEnd = parseOptionalDate(end, "Data final inválida");
        if (rangeStart != null && rangeEnd != null && !rangeEnd.isAfter(rangeStart)) {
            throw badRequest("O fim do período deve ser posterior ao início");
        }

        List<CalendarEvent> candidates = rangeStart != null && rangeEnd != null
                ? calendarEventRepository
                    .findByStartAtLessThanAndEndAtGreaterThanOrderByStartAtAsc(rangeEnd, rangeStart)
                : calendarEventRepository.findAllByOrderByStartAtAsc();

        AudienceContext context = audienceContext(user);
        return candidates.stream()
                .filter(event -> canView(event, user, context))
                .filter(event -> matchesKind(event, kind, excludeKind))
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional
    public CalendarEventDTO createEvent(CreateEventBodyDTO body) {
        User user = requireEventCreator();
        CalendarEvent event = new CalendarEvent();
        applyBody(event, body, user);
        event.setCreatedByUserId(user.getId());
        event.setCreatedByRole(user.getRole().name().toLowerCase(Locale.ROOT));
        return mapToDTO(calendarEventRepository.saveAndFlush(event));
    }

    @Transactional(readOnly = true)
    public CalendarEventDTO getEvent(String eventId) {
        return mapToDTO(requireViewableEvent(eventId));
    }

    @Transactional
    public CalendarEventDTO updateEvent(String eventId, CreateEventBodyDTO body) {
        User user = requireEventCreator();
        CalendarEvent event = requireOwnedEvent(eventId, user);
        applyBody(event, body, user);
        return mapToDTO(calendarEventRepository.saveAndFlush(event));
    }

    @Transactional
    public void deleteEvent(String eventId) {
        User user = requireEventCreator();
        CalendarEvent event = requireOwnedEvent(eventId, user);
        calendarEventRepository.delete(event);
    }

    @Transactional
    public CalendarEventDTO publishEvent(String eventId) {
        User user = requireEventCreator();
        CalendarEvent event = requireOwnedEvent(eventId, user);
        event.setPublished(true);
        return mapToDTO(calendarEventRepository.saveAndFlush(event));
    }

    @Transactional(readOnly = true)
    public Map<String, List<Map<String, Object>>> getTargetsForCurrentUser() {
        User user = requireEventCreator();
        List<School> schools = allowedSchools(user);
        Set<String> schoolIds = schools.stream().map(School::getId).collect(Collectors.toSet());
        List<SchoolClass> classes = classRepository.findAll().stream()
                .filter(schoolClass -> schoolClass.getSchool() != null)
                .filter(schoolClass -> schoolIds.contains(schoolClass.getSchool().getId()))
                .sorted(Comparator.comparing(SchoolClass::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        Map<UUID, String> gradeNames = gradeRepository.findAll().stream()
                .collect(Collectors.toMap(
                        grade -> grade.getId(),
                        grade -> grade.getName() == null ? grade.getId().toString() : grade.getName(),
                        (left, right) -> left
                ));

        List<Map<String, Object>> schoolTargets = schools.stream()
                .sorted(Comparator.comparing(School::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(school -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", school.getId());
                    item.put("nome", school.getName());
                    item.put("target_type", "SCHOOL");
                    if (school.getCity() != null) {
                        item.put("city_id", school.getCity().getId());
                        item.put("municipio_nome", school.getCity().getName());
                    }
                    return item;
                })
                .toList();

        List<Map<String, Object>> classTargets = classes.stream()
                .map(schoolClass -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", schoolClass.getId());
                    item.put("nome", schoolClass.getName());
                    item.put("target_type", "CLASS");
                    item.put("escola_id", schoolClass.getSchool().getId());
                    item.put("escola_nome", schoolClass.getSchool().getName());
                    if (schoolClass.getGradeId() != null) {
                        item.put("serie_id", schoolClass.getGradeId().toString());
                        item.put(
                                "serie_nome",
                                gradeNames.getOrDefault(
                                        schoolClass.getGradeId(),
                                        schoolClass.getGradeId().toString()
                                )
                        );
                    }
                    return item;
                })
                .toList();

        List<Map<String, Object>> municipalityTargets = new ArrayList<>();
        if (user.getRole() == RoleEnum.ADMIN) {
            Map<String, Map<String, Object>> uniqueCities = new LinkedHashMap<>();
            for (School school : schools) {
                if (school.getCity() == null) continue;
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", school.getCity().getId());
                item.put("nome", school.getCity().getName());
                item.put("target_type", "MUNICIPALITY");
                uniqueCities.putIfAbsent(school.getCity().getId(), item);
            }
            municipalityTargets.addAll(uniqueCities.values());
        }

        Map<String, List<Map<String, Object>>> response = new LinkedHashMap<>();
        response.put("municipios", municipalityTargets);
        response.put("escolas", schoolTargets);
        response.put("turmas", classTargets);
        return response;
    }

    @Transactional
    public Map<String, Object> uploadFileResource(
            String eventId,
            MultipartFile file,
            String title,
            Integer sortOrder
    ) {
        User user = requireEventCreator();
        CalendarEvent event = requireOwnedEvent(eventId, user);
        if (file == null || file.isEmpty()) {
            throw badRequest("Selecione um arquivo não vazio");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw badRequest("O arquivo deve ter no máximo 10 MB");
        }

        String resourceId = UUID.randomUUID().toString();
        String originalName = safeFileName(file.getOriginalFilename());
        String objectKey = "calendar/" + safePathSegment(TenantContext.getCurrentTenant())
                + "/" + event.getId() + "/" + resourceId + "-" + originalName;
        try {
            minioService.uploadFile(
                    objectKey,
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType() == null ? "application/octet-stream" : file.getContentType()
            );
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível armazenar o anexo",
                    exception
            );
        }

        CalendarEventResource resource = new CalendarEventResource();
        resource.setId(resourceId);
        resource.setEvent(event);
        resource.setResourceType("file");
        resource.setTitle(normalizeOptional(title, 160, originalName));
        resource.setOriginalFilename(originalName);
        resource.setContentType(file.getContentType() == null
                ? "application/octet-stream"
                : file.getContentType());
        resource.setSizeBytes(file.getSize());
        resource.setSortOrder(sortOrder == null ? event.getResources().size() : sortOrder);
        resource.setMinioObjectName(objectKey);
        event.getResources().add(resource);
        calendarEventRepository.saveAndFlush(event);
        return publicResource(resourceToMap(resource));
    }

    @Transactional(readOnly = true)
    public FileDownload downloadFileResource(String eventId, String resourceId) {
        CalendarEvent event = requireViewableEvent(eventId);
        Map<String, Object> resource = findResource(event, resourceId, "file");
        String objectKey = stringValue(resource.get("object_key"));
        if (objectKey == null) {
            throw notFound("Anexo não encontrado");
        }
        try (var input = minioService.downloadFile(objectKey)) {
            return new FileDownload(
                    input.readAllBytes(),
                    stringValue(resource.get("content_type")),
                    stringValue(resource.get("file_name"))
            );
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível baixar o anexo",
                    exception
            );
        }
    }

    @Transactional
    public void deleteResource(String eventId, String resourceId) {
        User user = requireEventCreator();
        CalendarEvent event = requireOwnedEvent(eventId, user);
        Map<String, Object> resource = findResource(event, resourceId, null);
        String objectKey = stringValue(resource.get("object_key"));
        if (objectKey != null) {
            try {
                minioService.deleteFile(objectKey);
            } catch (Exception exception) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Não foi possível remover o anexo",
                        exception
                );
            }
        }
        event.getResources().removeIf(item -> Objects.equals(resourceId, item.getId()));
        calendarEventRepository.saveAndFlush(event);
    }

    @Transactional(readOnly = true)
    public void assertCanRead(String eventId) {
        requireViewableEvent(eventId);
    }

    private void applyBody(CalendarEvent event, CreateEventBodyDTO body, User user) {
        if (body == null) {
            throw badRequest("Dados do evento não informados");
        }
        String title = normalizeRequired(body.getTitle(), 160, "O título do evento é obrigatório");
        ZoneId zone = parseZone(body.getTimezone());
        OffsetDateTime start = parseDate(body.getStart_at(), zone, "Data de início inválida");
        OffsetDateTime end = parseDate(body.getEnd_at(), zone, "Data de fim inválida");
        if (!end.isAfter(start)) {
            throw badRequest("A data final deve ser posterior à data inicial");
        }

        List<Map<String, Object>> targets = validateAndMapTargets(body.getTargets(), user);
        event.setTitle(title);
        event.setDescription(normalizeOptional(body.getDescription(), 5000, null));
        event.setLocation(normalizeOptional(body.getLocation(), 255, null));
        event.setStartAt(start);
        event.setEndAt(end);
        event.setAllDay(body.isAll_day());
        event.setTimezone(zone.getId());
        event.setVisibilityScope(deriveVisibilityScope(targets));
        event.getTargets().clear();
        event.getTargets().addAll(toTargetEntities(event, targets));
        event.getResources().removeIf(resource ->
                "link".equalsIgnoreCase(resource.getResourceType()));
        event.getResources().addAll(
                toResourceEntities(event, validateLinkResources(body.getResources()))
        );
        if (body.getIs_published() != null) {
            event.setPublished(body.getIs_published());
        }
        event.setRecurrenceRule(normalizeOptional(body.getRecurrence_rule(), 255, null));
        if (body.getMetadata() != null) {
            event.setMetadata(new HashMap<>(body.getMetadata()));
        }
        event.setMunicipalityId(firstTargetId(targets, "MUNICIPALITY"));
        event.setSchoolId(firstTargetId(targets, "SCHOOL"));
    }

    private List<Map<String, Object>> validateAndMapTargets(
            List<CreateEventBodyDTO.CalendarTargetPayload> requestedTargets,
            User user
    ) {
        if (requestedTargets == null || requestedTargets.isEmpty()) {
            throw badRequest("Informe ao menos um destinatário");
        }

        Set<String> allowedSchoolIds = allowedSchools(user).stream()
                .map(School::getId)
                .collect(Collectors.toSet());
        List<SchoolClass> allowedClasses = classRepository.findAll().stream()
                .filter(item -> item.getSchool() != null)
                .filter(item -> allowedSchoolIds.contains(item.getSchool().getId()))
                .toList();
        Set<String> allowedClassIds = allowedClasses.stream()
                .map(SchoolClass::getId)
                .collect(Collectors.toSet());
        Set<String> allowedGradeIds = allowedClasses.stream()
                .map(SchoolClass::getGradeId)
                .filter(Objects::nonNull)
                .map(UUID::toString)
                .collect(Collectors.toSet());
        Set<String> allowedCityIds = allowedSchools(user).stream()
                .map(School::getCity)
                .filter(Objects::nonNull)
                .map(city -> city.getId())
                .collect(Collectors.toSet());

        List<Map<String, Object>> targets = new ArrayList<>();
        for (CreateEventBodyDTO.CalendarTargetPayload target : requestedTargets) {
            String type = normalizeTargetType(target.getTarget_type());
            String id = trimToNull(target.getTarget_id());
            if (!TARGET_TYPES.contains(type)) {
                throw badRequest("Tipo de destinatário inválido: " + type);
            }

            Map<String, Object> mapped = new LinkedHashMap<>();
            mapped.put("target_type", type);
            switch (type) {
                case "ALL" -> {
                    if (user.getRole() != RoleEnum.ADMIN && user.getRole() != RoleEnum.TECADM) {
                        throw forbidden("Seu perfil não pode enviar eventos para todo o município");
                    }
                }
                case "USER" -> {
                    if (!Objects.equals(user.getId(), id)) {
                        throw forbidden("Eventos individuais só podem ser criados para o próprio usuário");
                    }
                    mapped.put("target_id", id);
                }
                case "MUNICIPALITY" -> {
                    requireTargetId(id);
                    if (user.getRole() != RoleEnum.ADMIN || !allowedCityIds.contains(id)) {
                        throw forbidden("Município fora do seu escopo de acesso");
                    }
                    mapped.put("target_id", id);
                }
                case "SCHOOL" -> {
                    requireAllowedId(id, allowedSchoolIds, "Escola fora do seu escopo de acesso");
                    mapped.put("target_id", id);
                }
                case "GRADE" -> {
                    requireAllowedId(id, allowedGradeIds, "Série fora do seu escopo de acesso");
                    mapped.put("target_id", id);
                }
                case "CLASS" -> {
                    requireAllowedId(id, allowedClassIds, "Turma fora do seu escopo de acesso");
                    mapped.put("target_id", id);
                }
                case "ROLE_GROUP" -> {
                    String roleGroup = id == null ? null : id.toLowerCase(Locale.ROOT);
                    if (roleGroup == null || !ROLE_GROUPS.contains(roleGroup)) {
                        throw badRequest("Perfil de destinatário inválido");
                    }
                    if (!allowedRecipientRoleGroups(user.getRole()).contains(roleGroup)) {
                        throw forbidden("Seu perfil não pode enviar eventos para esse grupo de usuários");
                    }
                    mapped.put("target_id", roleGroup);
                    Map<String, List<String>> filters = sanitizeRoleFilters(
                            target.getFilters(),
                            roleGroup,
                            allowedSchoolIds,
                            allowedGradeIds,
                            allowedClassIds
                    );
                    boolean restrictedCreator = user.getRole() == RoleEnum.DIRETOR
                            || user.getRole() == RoleEnum.COORDENADOR
                            || user.getRole() == RoleEnum.PROFESSOR;
                    if (restrictedCreator && filters.values().stream().allMatch(Collection::isEmpty)) {
                        throw forbidden("Seu perfil deve limitar o envio por escola, série ou turma");
                    }
                    if (!filters.isEmpty()) {
                        mapped.put("filters", filters);
                    }
                }
                default -> throw badRequest("Tipo de destinatário inválido");
            }
            targets.add(mapped);
        }
        return targets;
    }

    private Set<String> allowedRecipientRoleGroups(RoleEnum creatorRole) {
        return switch (creatorRole) {
            case ADMIN, TECADM -> ROLE_GROUPS;
            case DIRETOR, COORDENADOR -> Set.of(
                    "diretor", "coordenador", "professor", "aluno"
            );
            case PROFESSOR -> Set.of("professor", "aluno");
            default -> Set.of();
        };
    }

    private Map<String, List<String>> sanitizeRoleFilters(
            Map<String, List<String>> requested,
            String roleGroup,
            Set<String> allowedSchoolIds,
            Set<String> allowedGradeIds,
            Set<String> allowedClassIds
    ) {
        if (requested == null || requested.isEmpty()) {
            return Map.of();
        }
        Set<String> acceptedKeys = Set.of("school_ids", "grade_ids", "class_ids");
        if (!acceptedKeys.containsAll(requested.keySet())) {
            throw badRequest("Filtro de destinatário inválido");
        }
        if (!"aluno".equals(roleGroup)
                && (requested.containsKey("grade_ids") || requested.containsKey("class_ids"))) {
            throw badRequest("Filtros de série e turma só podem ser usados para alunos");
        }

        Map<String, List<String>> filters = new LinkedHashMap<>();
        putValidatedFilter(filters, "school_ids", requested.get("school_ids"), allowedSchoolIds);
        putValidatedFilter(filters, "grade_ids", requested.get("grade_ids"), allowedGradeIds);
        putValidatedFilter(filters, "class_ids", requested.get("class_ids"), allowedClassIds);
        return filters;
    }

    private void putValidatedFilter(
            Map<String, List<String>> destination,
            String key,
            List<String> values,
            Set<String> allowed
    ) {
        if (values == null || values.isEmpty()) return;
        List<String> normalized = values.stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!allowed.containsAll(normalized)) {
            throw forbidden("O filtro contém itens fora do seu escopo de acesso");
        }
        if (!normalized.isEmpty()) {
            destination.put(key, normalized);
        }
    }

    private List<Map<String, Object>> validateLinkResources(
            List<CreateEventBodyDTO.EventResource> requested
    ) {
        if (requested == null || requested.isEmpty()) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        int index = 0;
        for (CreateEventBodyDTO.EventResource resource : requested) {
            if (resource == null) continue;
            String type = trimToNull(resource.getType());
            if (!"link".equalsIgnoreCase(type)) {
                throw badRequest("Apenas links podem ser enviados junto com os dados do evento");
            }
            String title = normalizeRequired(
                    resource.getTitle(),
                    160,
                    "Informe o título de todos os links"
            );
            String url = normalizeRequired(resource.getUrl(), 2048, "Informe a URL de todos os links");
            validateHttpUrl(url);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put(
                    "id",
                    trimToNull(resource.getId()) == null
                            ? UUID.randomUUID().toString()
                            : resource.getId().trim()
            );
            item.put("type", "link");
            item.put("title", title);
            item.put("url", url);
            item.put("sort_order", resource.getSort_order() == null ? index : resource.getSort_order());
            result.add(item);
            index++;
        }
        return result;
    }

    private boolean canView(CalendarEvent event, User user, AudienceContext context) {
        if (Objects.equals(event.getCreatedByUserId(), user.getId())) {
            return true;
        }
        if (!event.isPublished()) {
            return false;
        }
        return currentTargets(event).stream().anyMatch(target -> targetMatches(target, context));
    }

    private boolean targetMatches(Map<String, Object> target, AudienceContext context) {
        String type = normalizeTargetType(stringValue(target.get("target_type")));
        String id = stringValue(target.get("target_id"));
        return switch (type) {
            case "ALL" -> true;
            case "USER" -> Objects.equals(id, context.userId());
            case "MUNICIPALITY" -> Objects.equals(id, context.cityId());
            case "SCHOOL" -> context.schoolIds().contains(id);
            case "GRADE" -> Objects.equals(id, context.gradeId());
            case "CLASS" -> Objects.equals(id, context.classId());
            case "ROLE_GROUP" -> roleGroupMatches(target, id, context);
            default -> false;
        };
    }

    private boolean roleGroupMatches(
            Map<String, Object> target,
            String requestedRole,
            AudienceContext context
    ) {
        if (!Objects.equals(requestedRole, context.role())) {
            return false;
        }
        Object rawFilters = target.get("filters");
        if (!(rawFilters instanceof Map<?, ?> filters)) {
            return true;
        }
        List<String> schoolIds = stringList(filters.get("school_ids"));
        List<String> gradeIds = stringList(filters.get("grade_ids"));
        List<String> classIds = stringList(filters.get("class_ids"));
        return (schoolIds.isEmpty() || schoolIds.stream().anyMatch(context.schoolIds()::contains))
                && (gradeIds.isEmpty() || gradeIds.contains(context.gradeId()))
                && (classIds.isEmpty() || classIds.contains(context.classId()));
    }

    private AudienceContext audienceContext(User user) {
        Set<String> schoolIds = new HashSet<>();
        String gradeId = null;
        String classId = null;
        if (user.getRole() == RoleEnum.DIRETOR || user.getRole() == RoleEnum.COORDENADOR) {
            String managedSchool = permissionService.getCurrentManagedSchoolId();
            if (managedSchool != null) schoolIds.add(managedSchool);
        } else if (user.getRole() == RoleEnum.PROFESSOR) {
            schoolIds.addAll(schoolTeacherRepository.findSchoolIdsByTeacherUserId(user.getId()));
        } else if (user.getRole() == RoleEnum.ALUNO) {
            Student student = studentRepository.findByUserId(user.getId()).orElse(null);
            if (student != null) {
                if (student.getSchool() != null) schoolIds.add(student.getSchool().getId());
                if (student.getGradeId() != null) gradeId = student.getGradeId().toString();
                if (student.getSchoolClass() != null) classId = student.getSchoolClass().getId();
            }
        }
        return new AudienceContext(
                user.getId(),
                user.getRole().name().toLowerCase(Locale.ROOT),
                user.getCity() == null ? null : user.getCity().getId(),
                schoolIds,
                gradeId,
                classId
        );
    }

    private List<School> allowedSchools(User user) {
        if (user.getRole() == RoleEnum.ADMIN || user.getRole() == RoleEnum.TECADM) {
            return schoolRepository.findAll();
        }
        if (user.getRole() == RoleEnum.DIRETOR || user.getRole() == RoleEnum.COORDENADOR) {
            String schoolId = permissionService.getCurrentManagedSchoolId();
            return schoolId == null ? List.of() : schoolRepository.findById(schoolId).stream().toList();
        }
        if (user.getRole() == RoleEnum.PROFESSOR) {
            Set<String> schoolIds = new HashSet<>(
                    schoolTeacherRepository.findSchoolIdsByTeacherUserId(user.getId())
            );
            return schoolRepository.findAllById(schoolIds);
        }
        if (user.getRole() == RoleEnum.ALUNO) {
            return studentRepository.findByUserId(user.getId())
                    .map(Student::getSchool)
                    .map(List::of)
                    .orElseGet(List::of);
        }
        return List.of();
    }

    private CalendarEvent requireViewableEvent(String eventId) {
        User user = requireCurrentUser();
        CalendarEvent event = findEvent(eventId);
        if (!canView(event, user, audienceContext(user))) {
            throw notFound("Evento não encontrado");
        }
        return event;
    }

    private CalendarEvent requireOwnedEvent(String eventId, User user) {
        CalendarEvent event = findEvent(eventId);
        if (!Objects.equals(event.getCreatedByUserId(), user.getId())) {
            throw forbidden("Apenas o criador pode alterar este evento");
        }
        return event;
    }

    private CalendarEvent findEvent(String eventId) {
        String id = trimToNull(eventId);
        if (id == null) throw notFound("Evento não encontrado");
        return calendarEventRepository.findById(id)
                .orElseThrow(() -> notFound("Evento não encontrado"));
    }

    private User requireCurrentUser() {
        User user = permissionService.getCurrentUser();
        if (user == null || user.getRole() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário autenticado não encontrado");
        }
        return user;
    }

    private User requireEventCreator() {
        User user = requireCurrentUser();
        if (!EVENT_CREATORS.contains(user.getRole())) {
            throw forbidden("Seu perfil não pode criar ou gerenciar eventos administrativos");
        }
        return user;
    }

    private CalendarEventDTO mapToDTO(CalendarEvent event) {
        ZoneId zone = parseZone(event.getTimezone());
        Map<String, Object> extended = new LinkedHashMap<>();
        extended.put("description", event.getDescription());
        extended.put("location", event.getLocation());
        extended.put("visibility_scope", event.getVisibilityScope());
        extended.put("targets", currentTargets(event));
        extended.put("resources", currentResources(event).stream().map(this::publicResource).toList());
        extended.put("is_published", event.isPublished());
        extended.put("recurrence_rule", event.getRecurrenceRule());
        extended.put("metadata", event.getMetadata() == null ? Map.of() : event.getMetadata());

        return CalendarEventDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .start(formatDate(event.getStartAt(), zone))
                .end(formatDate(event.getEndAt(), zone))
                .allDay(event.isAllDay())
                .timezone(zone.getId())
                .created_by(event.getCreatedByUserId() == null
                        ? null
                        : CalendarEventDTO.CalendarCreatedBy.builder()
                            .id(event.getCreatedByUserId())
                            .role(event.getCreatedByRole())
                            .build())
                .extendedProps(extended)
                .build();
    }

    private List<Map<String, Object>> currentTargets(CalendarEvent event) {
        if (event.getTargets() == null) return List.of();
        return event.getTargets().stream().map(target -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", target.getId());
            item.put("target_type", target.getTargetType());
            if (target.getTargetId() != null) item.put("target_id", target.getTargetId());
            if (target.getTargetFilters() != null && !target.getTargetFilters().isEmpty()) {
                item.put("filters", target.getTargetFilters());
            }
            return item;
        }).toList();
    }

    private List<Map<String, Object>> currentResources(CalendarEvent event) {
        if (event.getResources() == null) return List.of();
        return event.getResources().stream().map(this::resourceToMap).toList();
    }

    private List<CalendarEventTarget> toTargetEntities(
            CalendarEvent event,
            List<Map<String, Object>> targets
    ) {
        return targets.stream().map(item -> {
            CalendarEventTarget target = new CalendarEventTarget();
            target.setEvent(event);
            target.setTargetType(stringValue(item.get("target_type")));
            target.setTargetId(stringValue(item.get("target_id")));
            Object filters = item.get("filters");
            Map<String, Object> mappedFilters = new LinkedHashMap<>();
            if (filters instanceof Map<?, ?> rawFilters) {
                rawFilters.forEach((key, value) ->
                        mappedFilters.put(String.valueOf(key), value));
            }
            target.setTargetFilters(mappedFilters);
            return target;
        }).toList();
    }

    private List<CalendarEventResource> toResourceEntities(
            CalendarEvent event,
            List<Map<String, Object>> resources
    ) {
        return resources.stream().map(item -> {
            CalendarEventResource resource = new CalendarEventResource();
            String id = stringValue(item.get("id"));
            if (id != null) resource.setId(id);
            resource.setEvent(event);
            resource.setResourceType(stringValue(item.get("type")));
            resource.setTitle(stringValue(item.get("title")));
            resource.setUrl(stringValue(item.get("url")));
            resource.setMinioObjectName(stringValue(item.get("object_key")));
            resource.setOriginalFilename(stringValue(item.get("file_name")));
            resource.setContentType(stringValue(item.get("content_type")));
            Object size = item.get("size");
            if (size instanceof Number number) resource.setSizeBytes(number.longValue());
            Object order = item.get("sort_order");
            if (order instanceof Number number) resource.setSortOrder(number.intValue());
            return resource;
        }).toList();
    }

    private Map<String, Object> resourceToMap(CalendarEventResource resource) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", resource.getId());
        item.put("type", resource.getResourceType());
        item.put("title", resource.getTitle());
        if (resource.getUrl() != null) item.put("url", resource.getUrl());
        if (resource.getOriginalFilename() != null) {
            item.put("file_name", resource.getOriginalFilename());
        }
        if (resource.getContentType() != null) item.put("content_type", resource.getContentType());
        if (resource.getSizeBytes() != null) item.put("size", resource.getSizeBytes());
        item.put("sort_order", resource.getSortOrder());
        if (resource.getMinioObjectName() != null) {
            item.put("object_key", resource.getMinioObjectName());
        }
        return item;
    }

    private Map<String, Object> publicResource(Map<String, Object> resource) {
        Map<String, Object> result = new LinkedHashMap<>(resource);
        result.remove("object_key");
        return result;
    }

    private Map<String, Object> findResource(
            CalendarEvent event,
            String resourceId,
            String expectedType
    ) {
        return currentResources(event).stream()
                .filter(item -> Objects.equals(resourceId, stringValue(item.get("id"))))
                .filter(item -> expectedType == null
                        || expectedType.equalsIgnoreCase(stringValue(item.get("type"))))
                .findFirst()
                .orElseThrow(() -> notFound("Recurso não encontrado"));
    }

    private boolean matchesKind(CalendarEvent event, String kind, String excludeKind) {
        String eventKind = event.getMetadata() == null
                ? null
                : stringValue(event.getMetadata().get("kind"));
        String wanted = trimToNull(kind);
        String excluded = trimToNull(excludeKind);
        return (wanted == null || wanted.equalsIgnoreCase(eventKind))
                && (excluded == null || !excluded.equalsIgnoreCase(eventKind));
    }

    private String deriveVisibilityScope(List<Map<String, Object>> targets) {
        Set<String> types = targets.stream()
                .map(target -> normalizeTargetType(stringValue(target.get("target_type"))))
                .collect(Collectors.toSet());
        if (types.contains("CLASS")) return "CLASS";
        if (types.contains("GRADE")) return "GRADE";
        if (types.contains("SCHOOL")) return "SCHOOL";
        if (types.contains("USER")) return "USERS";
        if (types.contains("MUNICIPALITY") || types.contains("ALL") || types.contains("ROLE_GROUP")) {
            return "MUNICIPALITY";
        }
        return "USERS";
    }

    private String firstTargetId(List<Map<String, Object>> targets, String targetType) {
        return targets.stream()
                .filter(target -> targetType.equalsIgnoreCase(
                        stringValue(target.get("target_type"))))
                .map(target -> stringValue(target.get("target_id")))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private OffsetDateTime parseOptionalDate(String value, String message) {
        if (trimToNull(value) == null) return null;
        return parseDate(value, DEFAULT_ZONE, message);
    }

    private OffsetDateTime parseDate(String value, ZoneId zone, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) throw badRequest(message);
        try {
            return OffsetDateTime.parse(normalized).atZoneSameInstant(zone).toOffsetDateTime();
        } catch (DateTimeParseException ignored) {
            try {
                return java.time.LocalDateTime.parse(normalized).atZone(zone).toOffsetDateTime();
            } catch (DateTimeParseException exception) {
                throw badRequest(message);
            }
        }
    }

    private String formatDate(OffsetDateTime value, ZoneId zone) {
        return value == null ? null : value.atZoneSameInstant(zone).toOffsetDateTime().toString();
    }

    private ZoneId parseZone(String timezone) {
        String normalized = trimToNull(timezone);
        if (normalized == null) return DEFAULT_ZONE;
        try {
            return ZoneId.of(normalized);
        } catch (DateTimeException exception) {
            throw badRequest("Fuso horário inválido");
        }
    }

    private void validateHttpUrl(String value) {
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            if (uri.getHost() == null
                    || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw badRequest("Os links devem usar uma URL http ou https válida");
            }
        } catch (IllegalArgumentException exception) {
            throw badRequest("Os links devem usar uma URL http ou https válida");
        }
    }

    private String normalizeRequired(String value, int max, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) throw badRequest(message);
        if (normalized.length() > max) throw badRequest("O valor informado excede o limite permitido");
        return normalized;
    }

    private String normalizeOptional(String value, int max, String fallback) {
        String normalized = trimToNull(value);
        if (normalized == null) return fallback;
        if (normalized.length() > max) throw badRequest("O valor informado excede o limite permitido");
        return normalized;
    }

    private String normalizeTargetType(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? "" : normalized.toUpperCase(Locale.ROOT);
    }

    private void requireTargetId(String id) {
        if (id == null) throw badRequest("O identificador do destinatário é obrigatório");
    }

    private void requireAllowedId(String id, Set<String> allowed, String message) {
        requireTargetId(id);
        if (!allowed.contains(id)) throw forbidden(message);
    }

    private String safeFileName(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) return "arquivo";
        normalized = normalized.replace("\\", "/");
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1);
        normalized = normalized.replaceAll("[^\\p{L}\\p{N}._ -]", "_");
        return normalized.length() > 180 ? normalized.substring(normalized.length() - 180) : normalized;
    }

    private String safePathSegment(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? "tenant" : normalized.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String stringValue(Object value) {
        return value == null ? null : trimToNull(String.valueOf(value));
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof Collection<?> collection)) return List.of();
        return collection.stream()
                .map(this::stringValue)
                .filter(Objects::nonNull)
                .toList();
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    public record FileDownload(byte[] content, String contentType, String fileName) {}

    private record AudienceContext(
            String userId,
            String role,
            String cityId,
            Set<String> schoolIds,
            String gradeId,
            String classId
    ) {}
}
