package com.gcoedu.core.controller;

import com.gcoedu.core.domain.dto.tenant.TeacherDTO;
import com.gcoedu.core.domain.entity.publics.RoleEnum;
import com.gcoedu.core.domain.entity.publics.User;
import com.gcoedu.core.repository.publics.UserRepository;
import com.gcoedu.core.service.AuthService;
import com.gcoedu.core.service.tenant.TeacherService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = {"/api/v1/tenant/teachers", "/v1/tenant/teachers", "/teachers", "/api/teachers", "/teacher", "/api/teacher"})
public class TeacherController {

    private final TeacherService teacherService;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final com.gcoedu.core.repository.tenant.TeacherRepository teacherRepository;
    private final com.gcoedu.core.repository.publics.CityRepository cityRepository;
    private final com.gcoedu.core.repository.tenant.SchoolTeacherRepository schoolTeacherRepository;

    public TeacherController(TeacherService teacherService, UserRepository userRepository, AuthService authService, org.springframework.security.crypto.password.PasswordEncoder passwordEncoder, com.gcoedu.core.repository.tenant.TeacherRepository teacherRepository, com.gcoedu.core.repository.publics.CityRepository cityRepository, com.gcoedu.core.repository.tenant.SchoolTeacherRepository schoolTeacherRepository) {
        this.teacherService = teacherService;
        this.userRepository = userRepository;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
        this.teacherRepository = teacherRepository;
        this.cityRepository = cityRepository;
        this.schoolTeacherRepository = schoolTeacherRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TECADM')")
    public ResponseEntity<List<TeacherDTO>> getAllTeachers() {
        return ResponseEntity.ok(teacherService.findAll());
    }

    @GetMapping("/school/{schoolId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TECADM', 'DIRETOR', 'COORDENADOR')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getTeachersBySchool(@PathVariable String schoolId) {
        List<com.gcoedu.core.domain.entity.tenant.Teacher> teachers = schoolTeacherRepository.findTeachersBySchoolId(schoolId);
        
        List<Map<String, Object>> result = teachers.stream().map(t -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", t.getId());
            map.put("name", t.getName());
            map.put("registration", t.getRegistration());
            if (t.getUser() != null) {
                map.put("email", t.getUser().getEmail());
                map.put("role", t.getUser().getRole() != null ? t.getUser().getRole().name().toLowerCase() : "professor");
            } else {
                map.put("email", "");
                map.put("role", "professor");
            }
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TEACHER')")
    public ResponseEntity<TeacherDTO> getTeacherById(@PathVariable String id) {
        return ResponseEntity.ok(teacherService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TECADM')")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Object> createTeacher(@RequestBody Map<String, Object> payload) {
        String name = (String) payload.get("nome");
        String email = (String) payload.get("email");
        String password = (String) payload.get("senha");
        String registration = (String) payload.get("matricula");
        String birthDateStr = (String) payload.get("birth_date");
        String cityId = (String) payload.get("city_id");

        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "O nome é obrigatório"));
        }

        // Handle User creation or fetch
        User user = null;
        if (email != null && !email.trim().isEmpty()) {
            user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                user = new User();
                user.setId(java.util.UUID.randomUUID().toString());
                user.setName(name);
                user.setEmail(email);
                if (password != null && !password.isEmpty()) {
                    user.setPasswordHash(passwordEncoder.encode(password));
                }
                user.setRegistration((registration != null && !registration.trim().isEmpty()) ? registration : null);
                if (birthDateStr != null && !birthDateStr.isEmpty()) {
                    user.setBirthDate(java.time.LocalDate.parse(birthDateStr));
                }
                user.setRole(RoleEnum.PROFESSOR);
                if (cityId != null && !cityId.isEmpty()) {
                    user.setCity(cityRepository.getReferenceById(cityId));
                }
                userRepository.save(user);
            }
        }

        com.gcoedu.core.domain.entity.tenant.Teacher teacher = new com.gcoedu.core.domain.entity.tenant.Teacher();
        teacher.setId(java.util.UUID.randomUUID().toString());
        teacher.setName(name);
        teacher.setRegistration((registration != null && !registration.trim().isEmpty()) ? registration : null);
        if (birthDateStr != null && !birthDateStr.isEmpty()) {
            teacher.setBirthDate(java.time.LocalDate.parse(birthDateStr));
        }
        if (user != null) {
            teacher.setUser(user);
        }
        
        com.gcoedu.core.domain.entity.tenant.Teacher saved = teacherRepository.save(teacher);

        Object escolasIdsObj = payload.get("escolas_ids");
        if (escolasIdsObj instanceof java.util.List) {
            java.util.List<?> escolasIds = (java.util.List<?>) escolasIdsObj;
            for (Object schoolIdObj : escolasIds) {
                if (schoolIdObj instanceof String schoolIdStr) {
                    com.gcoedu.core.domain.entity.tenant.SchoolTeacher st = new com.gcoedu.core.domain.entity.tenant.SchoolTeacher();
                    st.setTeacher(saved);
                    com.gcoedu.core.domain.entity.tenant.School school = new com.gcoedu.core.domain.entity.tenant.School();
                    school.setId(schoolIdStr);
                    st.setSchool(school);
                    schoolTeacherRepository.save(st);
                }
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", saved.getId());
        resp.put("name", saved.getName());
        resp.put("email", user != null ? user.getEmail() : null);
        resp.put("registration", saved.getRegistration());
        resp.put("user_id", user != null ? user.getId() : null);

        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TEACHER')")
    public ResponseEntity<TeacherDTO> updateTeacher(@PathVariable String id, @Valid @RequestBody TeacherDTO dto) {
        return ResponseEntity.ok(teacherService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TECADM')")
    public ResponseEntity<Void> deleteTeacher(@PathVariable String id) {
        teacherService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ─── GET /teacher/directors ──────────────────────────────────────────────
    @GetMapping("/directors")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECADM', 'MANAGER')")
    public ResponseEntity<Object> listDirectors(Authentication auth) {
        return ResponseEntity.ok(listUsersByRole(auth, RoleEnum.DIRETOR, "diretores"));
    }

    // ─── GET /teacher/coordinators ───────────────────────────────────────────
    @GetMapping("/coordinators")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECADM', 'MANAGER')")
    public ResponseEntity<Object> listCoordinators(Authentication auth) {
        return ResponseEntity.ok(listUsersByRole(auth, RoleEnum.COORDENADOR, "coordenadores"));
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    private Map<String, Object> listUsersByRole(Authentication auth, RoleEnum role, String key) {
        User loggedUser = authService.getUserFromAuthentication(auth);

        List<User> all = userRepository.findAll().stream()
                .filter(u -> u.getRole() == role)
                .collect(Collectors.toList());

        // Filter by city if not admin
        if (loggedUser != null && loggedUser.getRole() != RoleEnum.ADMIN && loggedUser.getCity() != null) {
            String cityId = loggedUser.getCity().getId();
            all = all.stream()
                    .filter(u -> u.getCity() != null && cityId.equals(u.getCity().getId()))
                    .collect(Collectors.toList());
        }

        List<Map<String, Object>> result = all.stream().map(u -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("name", u.getName());
            map.put("email", u.getEmail());
            map.put("registration", u.getRegistration());
            map.put("role", u.getRole() != null ? u.getRole().name().toLowerCase() : null);
            map.put("city_id", u.getCity() != null ? u.getCity().getId() : null);
            return map;
        }).collect(Collectors.toList());

        return Map.of(key, result);
    }
}
