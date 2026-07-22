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

    public TeacherController(TeacherService teacherService, UserRepository userRepository, AuthService authService) {
        this.teacherService = teacherService;
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TECADM')")
    public ResponseEntity<List<TeacherDTO>> getAllTeachers() {
        return ResponseEntity.ok(teacherService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TEACHER')")
    public ResponseEntity<TeacherDTO> getTeacherById(@PathVariable String id) {
        return ResponseEntity.ok(teacherService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'TECADM')")
    public ResponseEntity<TeacherDTO> createTeacher(@Valid @RequestBody TeacherDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teacherService.create(dto));
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
