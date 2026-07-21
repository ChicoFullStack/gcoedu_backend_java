package com.gcoedu.core.service.tenant;

import com.gcoedu.core.domain.entity.publics.City;
import com.gcoedu.core.domain.entity.publics.Grade;
import com.gcoedu.core.domain.entity.publics.User;
import com.gcoedu.core.domain.entity.publics.RoleEnum;
import com.gcoedu.core.domain.entity.tenant.School;
import com.gcoedu.core.domain.entity.tenant.SchoolClass;
import com.gcoedu.core.domain.entity.tenant.Student;
import com.gcoedu.core.domain.entity.tenant.StudentPasswordLog;
import com.gcoedu.core.repository.publics.CityRepository;
import com.gcoedu.core.repository.publics.GradeRepository;
import com.gcoedu.core.repository.publics.UserRepository;
import com.gcoedu.core.repository.tenant.ClassRepository;
import com.gcoedu.core.repository.tenant.SchoolRepository;
import com.gcoedu.core.repository.tenant.StudentRepository;
import com.gcoedu.core.repository.tenant.StudentPasswordLogRepository;
import com.gcoedu.core.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkUploadService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final SchoolRepository schoolRepository;
    private final ClassRepository classRepository;
    private final GradeRepository gradeRepository;
    private final StudentPasswordLogRepository studentPasswordLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionService permissionService;
    private final com.gcoedu.core.repository.publics.ManagerRepository managerRepository;

    @Transactional
    public Map<String, Object> uploadStudents(MultipartFile file, String targetClassId) {
        try {
            if (file.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nenhum arquivo enviado");
            }

            String filename = file.getOriginalFilename();
            String extension = "";
            if (filename != null && filename.contains(".")) {
                extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
            }

            if (!extension.equals("csv")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de arquivo não suportado. Por enquanto use apenas CSV.");
            }

            // Get file content
            byte[] bytes = file.getBytes();
            String csvContent;
            try {
                csvContent = new String(bytes, StandardCharsets.UTF_8);
            } catch (Exception e) {
                csvContent = new String(bytes, StandardCharsets.ISO_8859_1);
            }

            List<Map<String, String>> rows = parseCsv(csvContent);

            // Fetch current user and check tenant/city permissions
            User currentUser = permissionService.getCurrentUser();
            if (currentUser == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
            }

            String currentCityId = permissionService.getCurrentUserCityId();
            if (currentCityId == null || currentCityId.isEmpty() || currentCityId.equalsIgnoreCase("public")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "É necessário estar vinculado a um município para realizar esta operação.");
            }

            // Resolve target class if classId is provided
            SchoolClass targetClass = null;
            School targetSchool = null;
            Grade targetGrade = null;

            if (targetClassId != null && !targetClassId.isEmpty()) {
                targetClass = classRepository.findById(targetClassId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma informada não encontrada"));
                targetSchool = targetClass.getSchool();
                if (targetSchool == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Escola da turma informada não encontrada");
                }
                if (targetClass.getGradeId() != null) {
                    targetGrade = gradeRepository.findById(targetClass.getGradeId())
                            .orElse(null);
                }
            }

            // Allowed schools based on user role
            List<String> allowedSchoolIds = new ArrayList<>();
            if (currentUser.getRole() == RoleEnum.ADMIN) {
                allowedSchoolIds = schoolRepository.findAll().stream().map(School::getId).collect(Collectors.toList());
            } else if (currentUser.getRole() == RoleEnum.TECADM) {
                allowedSchoolIds = schoolRepository.findByCityId(currentCityId).stream().map(School::getId).collect(Collectors.toList());
            } else if (currentUser.getRole() == RoleEnum.DIRETOR || currentUser.getRole() == RoleEnum.COORDENADOR) {
                // Fetch manager mappings
                Optional<com.gcoedu.core.domain.entity.publics.Manager> managerOpt = managerRepository.findByUserId(currentUser.getId());
                if (managerOpt.isPresent() && managerOpt.get().getSchoolId() != null) {
                    allowedSchoolIds.add(managerOpt.get().getSchoolId());
                }
            }

            if (allowedSchoolIds.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário não tem permissão para criar alunos em nenhuma escola");
            }

            if (targetClass != null && !allowedSchoolIds.contains(targetClass.getSchool().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão para criar alunos nesta turma");
            }

            // Set of used emails inside this batch to avoid duplicates
            Set<String> emailsUsedInBatch = new HashSet<>();
            // Gather all used pins/registrations in the database
            Set<String> usedPins = studentRepository.findAll().stream()
                    .map(Student::getRegistration)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .collect(Collectors.toSet());

            int successes = 0;
            List<Map<String, String>> errors = new ArrayList<>();
            List<Map<String, String>> createdStudents = new ArrayList<>();

            for (int i = 0; i < rows.size(); i++) {
                Map<String, String> row = rows.get(i);
                int lineNumber = i + 2;

                try {
                    String fullName = row.get("nome");
                    if (fullName == null || fullName.trim().isEmpty()) {
                        addError(errors, lineNumber, "nome", "", "Nome é obrigatório");
                        continue;
                    }
                    fullName = fullName.trim();

                    // Generate email from initials
                    String baseLocal = generateEmailInitials(fullName);
                    if (baseLocal.isEmpty()) {
                        addError(errors, lineNumber, "nome", fullName, "Não foi possível gerar iniciais para o email");
                        continue;
                    }

                    String email = getAvailableEmail(baseLocal, "@gcoedu.com.br", emailsUsedInBatch);
                    emailsUsedInBatch.add(email.toLowerCase());

                    // Generate password: first name + @gcoedu
                    String firstName = getFirstName(fullName);
                    String password = firstName.isEmpty() ? "aluno@gcoedu" : firstName + "@gcoedu";

                    School school = null;
                    Grade grade = null;
                    SchoolClass schoolClass = null;

                    if (targetClass != null) {
                        school = targetSchool;
                        grade = targetGrade;
                        schoolClass = targetClass;
                    } else {
                        // Resolve school
                        String schoolName = row.get("escola");
                        if (schoolName == null || schoolName.trim().isEmpty()) {
                            addError(errors, lineNumber, "escola", "", "Escola é obrigatória");
                            continue;
                        }
                        String normalizedSchool = normalizeNameForSearch(schoolName);

                        for (School s : schoolRepository.findAll()) {
                            if (normalizeNameForSearch(s.getName()).equals(normalizedSchool)) {
                                school = s;
                                break;
                            }
                        }

                        if (school == null) {
                            addError(errors, lineNumber, "escola", schoolName, "Escola não encontrada");
                            continue;
                        }

                        if (!allowedSchoolIds.contains(school.getId())) {
                            addError(errors, lineNumber, "escola", schoolName, "Sem permissão para criar alunos nesta escola");
                            continue;
                        }

                        // Resolve grade
                        String gradeIdRaw = row.get("grade_id");
                        String gradeName = row.get("serie");
                        if (gradeIdRaw == null || gradeIdRaw.trim().isEmpty()) {
                            addError(errors, lineNumber, "grade_id", "", "grade_id é obrigatório");
                            continue;
                        }

                        try {
                            grade = gradeRepository.findById(UUID.fromString(gradeIdRaw.trim()))
                                    .orElse(null);
                        } catch (Exception ex) {
                            // ignore parsing error, handled below
                        }

                        if (grade == null) {
                            addError(errors, lineNumber, "grade_id", gradeIdRaw, "Série não encontrada");
                            continue;
                        }

                        // Resolve class
                        String className = row.get("turma");
                        if (className == null || className.trim().isEmpty()) {
                            addError(errors, lineNumber, "turma", "", "Turma é obrigatória");
                            continue;
                        }
                        String normalizedClass = normalizeNameForSearch(className);

                        List<SchoolClass> candidates = classRepository.findBySchoolId(school.getId());
                        for (SchoolClass c : candidates) {
                            if (c.getGradeId() != null && c.getGradeId().equals(grade.getId())) {
                                if (normalizeNameForSearch(c.getName()).equals(normalizedClass)) {
                                    schoolClass = c;
                                    break;
                                }
                            }
                        }

                        if (schoolClass == null) {
                            addError(errors, lineNumber, "turma", className, "Turma não encontrada na escola " + school.getName() + " e série " + grade.getName());
                            continue;
                        }
                    }

                    // Parse birth date (optional)
                    LocalDate birthDate = parseDate(row.get("data_nascimento"));

                    // Create User
                    User user = new User();
                    user.setId(UUID.randomUUID().toString());
                    user.setName(fullName);
                    user.setEmail(email);
                    user.setPasswordHash(passwordEncoder.encode(password));
                    user.setRole(RoleEnum.ALUNO);
                    if (school.getCity() != null) {
                        user.setCity(school.getCity());
                    }
                    userRepository.save(user);

                    // Create Student
                    Student student = new Student();
                    student.setId(UUID.randomUUID().toString());
                    student.setName(fullName);
                    student.setBirthDate(birthDate);
                    student.setUser(user);
                    student.setSchool(school);
                    if (grade.getId() != null) {
                        student.setGradeId(grade.getId());
                    }
                    student.setSchoolClass(schoolClass);

                    // Assign Pin
                    String pin = allocateUniquePin(usedPins);
                    student.setRegistration(pin);
                    studentRepository.save(student);

                    // Create StudentPasswordLog
                    StudentPasswordLog passwordLog = new StudentPasswordLog();
                    passwordLog.setId(UUID.randomUUID().toString());
                    passwordLog.setStudentName(fullName);
                    passwordLog.setEmail(email);
                    passwordLog.setPassword(password);
                    passwordLog.setRegistration(pin);
                    passwordLog.setUserId(user.getId());
                    passwordLog.setStudentId(student.getId());
                    passwordLog.setClassId(UUID.fromString(schoolClass.getId()));
                    if (grade.getId() != null) {
                        passwordLog.setGradeId(grade.getId());
                    }
                    passwordLog.setSchoolId(school.getId());
                    if (school.getCity() != null) {
                        passwordLog.setCityId(school.getCity().getId());
                    }
                    studentPasswordLogRepository.save(passwordLog);

                    // Prepare info
                    Map<String, String> info = new HashMap<>();
                    info.put("nome", fullName);
                    info.put("email", email);
                    info.put("senha", password);
                    info.put("matricula", pin);
                    info.put("escola", school.getName());
                    info.put("serie", grade.getName());
                    info.put("turma", schoolClass.getName());

                    createdStudents.add(info);
                    successes++;

                } catch (Exception e) {
                    addError(errors, lineNumber, "geral", "", "Erro inesperado: " + e.getMessage());
                }
            }

            String message;
            if (successes > 0) {
                message = "Upload concluído! " + successes + " alunos criados com sucesso.";
                if (!errors.isEmpty()) {
                    message += " " + errors.size() + " erros encontrados.";
                }
            } else {
                message = "Nenhum aluno foi criado. Verifique os erros abaixo.";
            }

            Map<String, Object> result = new HashMap<>();
            result.put("mensagem", message);
            result.put("resumo", Map.of(
                    "total_linhas", rows.size(),
                    "sucessos", successes,
                    "erros", errors.size()
            ));
            result.put("alunos_criados", createdStudents);
            result.put("erros", errors);

            return result;

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado no upload em massa", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor: " + e.getMessage());
        }
    }

    private void addError(List<Map<String, String>> errors, int line, String field, String value, String errorMsg) {
        Map<String, String> err = new HashMap<>();
        err.put("linha", String.valueOf(line));
        err.put("campo", field);
        err.put("valor", value);
        err.put("erro", errorMsg);
        errors.add(err);
    }

    private String normalizeNameForSearch(String name) {
        if (name == null) return "";
        return name.replaceAll("\\s+", " ").trim().toLowerCase();
    }

    private String generateEmailInitials(String fullName) {
        if (fullName == null) return "";
        String[] parts = fullName.replaceAll("\\s+", " ").trim().split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toLowerCase(part.charAt(0)));
            }
        }
        return sb.toString();
    }

    private String getFirstName(String fullName) {
        if (fullName == null) return "";
        String[] parts = fullName.replaceAll("\\s+", " ").trim().split(" ");
        return parts.length > 0 ? parts[0].toLowerCase() : "";
    }

    private String getAvailableEmail(String baseLocal, String domain, Set<String> usedInBatch) {
        String candidate = baseLocal + domain;
        if (!usedInBatch.contains(candidate.toLowerCase()) && userRepository.findByEmail(candidate).isEmpty()) {
            return candidate;
        }
        int suffix = 1;
        while (true) {
            candidate = baseLocal + suffix + domain;
            if (!usedInBatch.contains(candidate.toLowerCase()) && userRepository.findByEmail(candidate).isEmpty()) {
                return candidate;
            }
            suffix++;
        }
    }

    private String allocateUniquePin(Set<String> used) {
        if (used.size() >= 10000) {
            throw new RuntimeException("Esgotados os 10000 PINs possíveis neste município (schema).");
        }
        java.security.SecureRandom random = new java.security.SecureRandom();
        for (int i = 0; i < 2000; i++) {
            String pin = String.format("%04d", random.nextInt(10000));
            if (!used.contains(pin)) {
                used.add(pin);
                return pin;
            }
        }
        throw new RuntimeException("Não foi possível gerar PIN único após 2000 tentativas.");
    }

    private LocalDate parseDate(String dateValue) {
        if (dateValue == null || dateValue.trim().isEmpty()) {
            return null;
        }
        String dateStr = dateValue.trim();
        List<String> formats = List.of("dd/mm/yyyy", "dd-mm-yyyy", "yyyy-mm-dd", "dd/mm/yy", "dd-mm-yy");
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("d-M-yyyy"),
                DateTimeFormatter.ofPattern("yyyy-M-d"),
                DateTimeFormatter.ofPattern("d/M/yy"),
                DateTimeFormatter.ofPattern("d-M-yy")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (Exception e) {
                // continue trying next format
            }
        }
        return null;
    }

    private List<Map<String, String>> parseCsv(String csvContent) throws Exception {
        List<Map<String, String>> rows = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new StringReader(csvContent));
        String headerLine = reader.readLine();
        if (headerLine == null) {
            return rows;
        }

        String delimiter = ",";
        if (headerLine.contains(";") && !headerLine.contains(",")) {
            delimiter = ";";
        } else if (headerLine.contains("\t")) {
            delimiter = "\t";
        }

        List<String> headers = parseCsvLine(headerLine, delimiter);
        for (int i = 0; i < headers.size(); i++) {
            String h = headers.get(i);
            if (h != null) {
                h = h.replace("\ufeff", "").replace("\"", "").trim().toLowerCase();
                headers.set(i, h);
            }
        }

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) {
                continue;
            }
            List<String> values = parseCsvLine(line, delimiter);
            Map<String, String> row = new HashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                if (i < values.size()) {
                    row.put(headers.get(i), values.get(i));
                } else {
                    row.put(headers.get(i), "");
                }
            }
            rows.add(row);
        }
        return rows;
    }

    private List<String> parseCsvLine(String line, String delimiter) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        char dChar = delimiter.charAt(0);
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == dChar && !inQuotes) {
                result.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString().trim());
        for (int i = 0; i < result.size(); i++) {
            String val = result.get(i);
            if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                result.set(i, val.substring(1, val.length() - 1).trim());
            }
        }
        return result;
    }
}
