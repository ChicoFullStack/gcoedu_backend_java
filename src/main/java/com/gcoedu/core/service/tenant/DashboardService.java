package com.gcoedu.core.service.tenant;

import com.gcoedu.core.config.tenant.TenantContext;
import com.gcoedu.core.domain.dto.tenant.BaseDashboardResponseDTO;
import com.gcoedu.core.domain.dto.tenant.DashboardDTO;
import com.gcoedu.core.domain.dto.tenant.EvaluationStatsResponseDTO;
import com.gcoedu.core.domain.entity.publics.Grade;
import com.gcoedu.core.domain.entity.publics.RoleEnum;
import com.gcoedu.core.domain.entity.publics.User;
import com.gcoedu.core.domain.entity.tenant.Question;
import com.gcoedu.core.domain.entity.tenant.SchoolClass;
import com.gcoedu.core.domain.entity.tenant.Student;
import com.gcoedu.core.domain.entity.tenant.Test;
import com.gcoedu.core.repository.publics.GradeRepository;
import com.gcoedu.core.repository.publics.ManagerRepository;
import com.gcoedu.core.repository.publics.UserRepository;
import com.gcoedu.core.repository.tenant.*;
import com.gcoedu.core.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final String EVALUATION_TYPE = "AVALIACAO";
    private static final ZoneId APP_ZONE = ZoneId.of("America/Sao_Paulo");

    private final StudentRepository studentRepository;
    private final SchoolRepository schoolRepository;
    private final TestRepository testRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final ClassRepository classRepository;
    private final TeacherRepository teacherRepository;
    private final SchoolTeacherRepository schoolTeacherRepository;
    private final AnswerSheetResultRepository resultRepository;
    private final TestSessionRepository testSessionRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final GradeRepository gradeRepository;
    private final ManagerRepository managerRepository;
    private final PermissionService permissionService;

    public BaseDashboardResponseDTO getComprehensiveStats() {
        ScopedCounts counts = scopedCounts();
        return BaseDashboardResponseDTO.builder()
                .students(counts.students())
                .schools(counts.schools())
                .evaluations(counts.evaluations())
                .games(0)
                .users(counts.users())
                .questions(questionRepository.count())
                .classes(counts.classes())
                .teachers(counts.teachers())
                .last_sync(LocalDateTime.now(APP_ZONE).toString())
                .build();
    }

    public EvaluationStatsResponseDTO getEvaluationStats() {
        ScopedCounts counts = scopedCounts();
        long totalQuestions = questionRepository.count();
        double averageQuestions = counts.evaluations() > 0
                ? (double) totalQuestions / counts.evaluations()
                : 0.0;
        return EvaluationStatsResponseDTO.builder()
                .total(counts.evaluations())
                .this_month(counts.evaluations())
                .total_questions(totalQuestions)
                .average_questions(averageQuestions)
                .build();
    }

    public DashboardDTO.RankingResponse<DashboardDTO.StudentRankingItem> getRankingAlunos(
            int limit, int offset) {
        User user = requireCurrentUser();
        PageRequest pageable = pageRequest(limit, offset);
        Page<Object[]> page;

        if (user.getRole() == RoleEnum.DIRETOR || user.getRole() == RoleEnum.COORDENADOR) {
            String schoolId = permissionService.getCurrentManagedSchoolId();
            if (schoolId == null) {
                return new DashboardDTO.RankingResponse<>(List.of(), 0, limit, offset);
            }
            page = resultRepository.findTopStudentsRankingBySchool(schoolId, pageable);
        } else if (user.getRole() == RoleEnum.PROFESSOR) {
            List<String> schoolIds = schoolTeacherRepository.findSchoolIdsByTeacherUserId(user.getId());
            if (schoolIds.isEmpty()) {
                return new DashboardDTO.RankingResponse<>(List.of(), 0, limit, offset);
            }
            page = resultRepository.findTopStudentsRankingBySchools(schoolIds, pageable);
        } else if (user.getRole() == RoleEnum.ALUNO) {
            Student student = studentRepository.findByUserId(user.getId()).orElse(null);
            String classId = student != null && student.getSchoolClass() != null
                    ? student.getSchoolClass().getId()
                    : null;
            if (classId == null) {
                return new DashboardDTO.RankingResponse<>(List.of(), 0, limit, offset);
            }
            page = resultRepository.findTopStudentsRankingByClass(classId, pageable);
        } else {
            page = resultRepository.findTopStudentsRanking(pageable);
        }

        List<DashboardDTO.StudentRankingItem> ranking = new ArrayList<>();
        Map<UUID, String> gradeNames = gradeNames();
        int position = offset + 1;
        for (Object[] row : page.getContent()) {
            Student student = (Student) row[0];
            SchoolClass schoolClass = student.getSchoolClass();
            ranking.add(new DashboardDTO.StudentRankingItem(
                    position++,
                    student.getId(),
                    defaultText(student.getName(), "Sem nome"),
                    gradeName(gradeNames, student.getGradeId()),
                    schoolClass != null ? defaultText(schoolClass.getName(), "-") : "-",
                    student.getSchool() != null ? defaultText(student.getSchool().getName(), "-") : "-",
                    number(row[1]),
                    integer(row[2])
            ));
        }
        return new DashboardDTO.RankingResponse<>(ranking, page.getTotalElements(), limit, offset);
    }

    public DashboardDTO.RankingResponse<DashboardDTO.ClassRankingItem> getRankingTurmas(
            int limit, int offset) {
        User user = requireCurrentUser();
        PageRequest pageable = pageRequest(limit, offset);
        Page<Object[]> page;

        if (user.getRole() == RoleEnum.DIRETOR || user.getRole() == RoleEnum.COORDENADOR) {
            String schoolId = permissionService.getCurrentManagedSchoolId();
            if (schoolId == null) {
                return new DashboardDTO.RankingResponse<>(List.of(), 0, limit, offset);
            }
            page = resultRepository.findTopClassesRankingBySchool(schoolId, pageable);
        } else {
            page = resultRepository.findTopClassesRanking(pageable);
        }

        List<DashboardDTO.ClassRankingItem> ranking = new ArrayList<>();
        Map<UUID, String> gradeNames = gradeNames();
        int position = offset + 1;
        for (Object[] row : page.getContent()) {
            SchoolClass schoolClass = (SchoolClass) row[0];
            int totalStudents = Math.toIntExact(studentRepository.countBySchoolClassId(schoolClass.getId()));
            int participatingStudents = integer(row[5]);
            double completion = totalStudents > 0
                    ? Math.min(100.0, participatingStudents * 100.0 / totalStudents)
                    : 0.0;
            ranking.add(new DashboardDTO.ClassRankingItem(
                    position++,
                    schoolClass.getId(),
                    defaultText(schoolClass.getName(), "Sem nome"),
                    gradeName(gradeNames, schoolClass.getGradeId()),
                    number(row[1]),
                    integer(row[2]),
                    number(row[3]),
                    completion,
                    totalStudents,
                    integer(row[4])
            ));
        }
        return new DashboardDTO.RankingResponse<>(ranking, page.getTotalElements(), limit, offset);
    }

    public DashboardDTO.RecentEvaluationsResponse getAvaliacoesRecentes(int limit) {
        User user = requireCurrentUser();
        PageRequest pageable = PageRequest.of(0, limit);
        Page<Test> page;
        if (user.getRole() == RoleEnum.ADMIN || user.getRole() == RoleEnum.TECADM) {
            page = testRepository.findByTypeIgnoreCaseOrderByCreatedAtDesc(EVALUATION_TYPE, pageable);
        } else {
            page = testRepository.findByTypeIgnoreCaseAndCreatorIdOrderByCreatedAtDesc(
                    EVALUATION_TYPE, user.getId(), pageable);
        }

        List<DashboardDTO.RecentEvaluationItem> evaluations = page.getContent().stream()
                .map(this::toRecentEvaluation)
                .toList();
        return new DashboardDTO.RecentEvaluationsResponse(evaluations);
    }

    public DashboardDTO.QuestionsResponse getQuestoes(int limit, int offset) {
        Page<Question> page = questionRepository.findAllByOrderByCreatedAtDesc(pageRequest(limit, offset));
        List<String> questionIds = page.getContent().stream()
                .map(Question::getId)
                .toList();
        Map<String, Long> evaluationCounts = questionIds.isEmpty()
                ? Map.of()
                : testQuestionRepository.countByQuestionIds(questionIds).stream()
                        .collect(Collectors.toMap(
                                row -> (String) row[0],
                                row -> ((Number) row[1]).longValue()));
        List<DashboardDTO.QuestionItem> questions = page.getContent().stream()
                .map(question -> new DashboardDTO.QuestionItem(
                        question.getId(),
                        defaultText(question.getStatement(), "Questão sem enunciado"),
                        question.getSubject() != null ? defaultText(question.getSubject().getName(), "-") : "-",
                        "-",
                        "GCOEdu",
                        question.getCreatedAt() != null ? question.getCreatedAt().toString() : "",
                        null,
                        null,
                        "multipla_escolha",
                        0,
                        null,
                        evaluationCounts.getOrDefault(question.getId(), 0L),
                        null,
                        null
                ))
                .toList();
        return new DashboardDTO.QuestionsResponse(
                questions, page.getTotalElements(), limit, offset);
    }

    public DashboardDTO.SystemAnalysis getSystemAnalysis() {
        User user = requireCurrentUser();
        if (user.getRole() != RoleEnum.ADMIN && user.getRole() != RoleEnum.TECADM) {
            throw new AccessDeniedException("Análise do sistema restrita a ADMIN e TECADM");
        }

        ScopedCounts counts = scopedCounts();
        long sessions = testSessionRepository.count();
        long completedSessions = testSessionRepository.countBySubmittedAtIsNotNull();
        long studentsWithSubmission = testSessionRepository.countDistinctStudentsWithSubmission();
        double completionRate = sessions > 0 ? completedSessions * 100.0 / sessions : 0.0;
        double participation = counts.students() > 0
                ? studentsWithSubmission * 100.0 / counts.students()
                : 0.0;
        String now = LocalDateTime.now(APP_ZONE).toString();

        DashboardDTO.Metricas metrics = new DashboardDTO.Metricas(
                counts.students(), counts.schools(), counts.evaluations(), 0,
                counts.users(), questionRepository.count(), counts.classes(),
                counts.teachers(), 0, now);
        DashboardDTO.Conexao connection = new DashboardDTO.Conexao(
                "PostgreSQL", "remoto", "conectado", now,
                activeProfile(), APP_ZONE.getId());
        DashboardDTO.DadosTecnicos technical = new DashboardDTO.DadosTecnicos(
                activeProfile(), now, APP_ZONE.getId());

        Map<String, Object> general = new LinkedHashMap<>();
        general.put("students", counts.students());
        general.put("schools", counts.schools());
        general.put("evaluations", counts.evaluations());
        Map<String, Object> scopes = new LinkedHashMap<>();
        scopes.put("geral", general);
        scopes.put("estado", List.of());
        scopes.put("municipio", List.of());
        scopes.put("escola", List.of());

        Map<String, Object> charts = new LinkedHashMap<>();
        charts.put("evolucao_ultimos_12_meses", List.of());
        charts.put("distribuicao_por_estado", List.of());
        charts.put("distribuicao_por_municipio", List.of());
        charts.put("avaliacoes_por_tipo", List.of());
        charts.put("participacao", Map.of(
                "total_alunos", counts.students(),
                "alunos_com_pelo_menos_uma_avaliacao", studentsWithSubmission,
                "percentual_participacao", participation));

        DashboardDTO.Administracao administration = new DashboardDTO.Administracao(
                completionRate, sessions, completedSessions, 0,
                studentsWithSubmission, participation, counts.schools(),
                now, countSubjectsWithQuestions());
        return new DashboardDTO.SystemAnalysis(
                metrics, connection, technical, scopes, charts, administration);
    }

    private DashboardDTO.RecentEvaluationItem toRecentEvaluation(Test test) {
        long expected = testSessionRepository.countByTestId(test.getId());
        long completed = testSessionRepository.countByTestIdAndSubmittedAtIsNotNull(test.getId());
        double progress = expected > 0 ? completed * 100.0 / expected : 0.0;
        List<String> schools = testSessionRepository.findDistinctSchoolNamesByTestId(test.getId());
        return new DashboardDTO.RecentEvaluationItem(
                test.getId(),
                defaultText(test.getTitle(), "Sem título"),
                completed,
                expected,
                test.getEndTime() != null ? test.getEndTime().toString() : null,
                progress,
                defaultText(test.getStatus(), "pendente"),
                test.getSubjectRel() != null ? defaultText(test.getSubjectRel().getName(), "-") : "-",
                schools.isEmpty() ? "-" : schools.get(0),
                schools,
                null,
                test.getCreatedAt() != null ? test.getCreatedAt().toString() : null
        );
    }

    private ScopedCounts scopedCounts() {
        User user = requireCurrentUser();
        RoleEnum role = user.getRole();
        if (role == RoleEnum.DIRETOR || role == RoleEnum.COORDENADOR) {
            String schoolId = permissionService.getCurrentManagedSchoolId();
            if (schoolId == null) {
                return new ScopedCounts(0, 0, 0, 0, 0, 0);
            }
            long students = studentRepository.countBySchoolId(schoolId);
            long teachers = schoolTeacherRepository.countBySchoolId(schoolId);
            long managers = managerRepository.countAllBySchoolId(schoolId);
            return new ScopedCounts(
                    students, 1, testRepository.countByCreatorId(user.getId()),
                    students + teachers + managers,
                    classRepository.countBySchoolId(schoolId), teachers);
        }

        if (role == RoleEnum.PROFESSOR) {
            List<String> schoolIds = schoolTeacherRepository.findSchoolIdsByTeacherUserId(user.getId());
            long students = schoolIds.stream().mapToLong(studentRepository::countBySchoolId).sum();
            long classes = schoolIds.stream().mapToLong(classRepository::countBySchoolId).sum();
            return new ScopedCounts(
                    students, schoolIds.size(), testRepository.countByCreatorId(user.getId()),
                    1, classes, 0);
        }

        if (role == RoleEnum.ALUNO) {
            Student student = studentRepository.findByUserId(user.getId()).orElse(null);
            long classes = student != null && student.getSchoolClass() != null ? 1 : 0;
            long schools = student != null && student.getSchool() != null ? 1 : 0;
            return new ScopedCounts(1, schools, 0, 1, classes, 0);
        }

        return new ScopedCounts(
                studentRepository.count(),
                schoolRepository.count(),
                testRepository.count(),
                scopedUserCount(user),
                classRepository.count(),
                teacherRepository.count());
    }

    private long scopedUserCount(User user) {
        String cityId = cityIdFromTenant();
        if (cityId == null && user.getCity() != null) {
            cityId = user.getCity().getId();
        }
        if (cityId != null) {
            return userRepository.countByCityId(cityId);
        }
        return user.getRole() == RoleEnum.ADMIN ? userRepository.count() : 1;
    }

    private String cityIdFromTenant() {
        String tenant = TenantContext.getCurrentTenant();
        if (tenant == null || !tenant.startsWith("city_")) {
            return null;
        }
        String value = tenant.substring("city_".length()).replace('_', '-');
        try {
            return UUID.fromString(value).toString();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Map<UUID, String> gradeNames() {
        return gradeRepository.findAll().stream()
                .collect(Collectors.toMap(
                        Grade::getId,
                        grade -> defaultText(grade.getName(), "-"),
                        (left, right) -> left));
    }

    private String gradeName(Map<UUID, String> gradeNames, UUID gradeId) {
        if (gradeId == null) {
            return "-";
        }
        return gradeNames.getOrDefault(gradeId, "-");
    }

    private long countSubjectsWithQuestions() {
        return questionRepository.findAll().stream()
                .map(Question::getSubject)
                .filter(Objects::nonNull)
                .map(subject -> subject.getId())
                .distinct()
                .count();
    }

    private User requireCurrentUser() {
        User user = permissionService.getCurrentUser();
        if (user == null) {
            throw new AccessDeniedException("Usuário autenticado não encontrado");
        }
        return user;
    }

    private PageRequest pageRequest(int limit, int offset) {
        return PageRequest.of(offset / limit, limit);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private double number(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }

    private int integer(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private String activeProfile() {
        return System.getProperty("spring.profiles.active", "default");
    }

    private record ScopedCounts(
            long students,
            long schools,
            long evaluations,
            long users,
            long classes,
            long teachers) {
    }
}
