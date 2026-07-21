package com.gcoedu.core.controller;

import com.gcoedu.core.domain.entity.publics.Manager;
import com.gcoedu.core.domain.entity.publics.RoleEnum;
import com.gcoedu.core.domain.entity.publics.User;
import com.gcoedu.core.repository.publics.CityRepository;
import com.gcoedu.core.repository.publics.ManagerRepository;
import com.gcoedu.core.repository.publics.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = {"/managers", "/api/managers"})
public class ManagerController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CityRepository cityRepository;
    private final ManagerRepository managerRepository;
    private final com.gcoedu.core.service.AuthService authService;

    public ManagerController(UserRepository userRepository,
                             PasswordEncoder passwordEncoder,
                             CityRepository cityRepository,
                             ManagerRepository managerRepository,
                             com.gcoedu.core.service.AuthService authService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.cityRepository = cityRepository;
        this.managerRepository = managerRepository;
        this.authService = authService;
    }

    // ─── GET /managers ─────────────────────────────────────────────────────────
    @GetMapping({"", "/"})
    @PreAuthorize("hasAnyRole('ADMIN', 'TECADM')")
    public ResponseEntity<Object> getManagers() {
        List<Manager> managers = managerRepository.findAll();
        return ResponseEntity.ok(buildManagerList(managers));
    }

    // ─── GET /managers/city/{cityId} ───────────────────────────────────────────
    @GetMapping("/city/{cityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECADM')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<Object> getManagersByCity(@PathVariable String cityId) {
        List<Manager> managers = managerRepository.findAll().stream()
                .filter(m -> m.getUser() != null
                        && m.getUser().getCity() != null
                        && cityId.equals(m.getUser().getCity().getId()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(buildManagerList(managers));
    }

    // ─── GET /managers/school/{schoolId} ──────────────────────────────────────
    @GetMapping("/school/{schoolId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECADM', 'MANAGER')")
    public ResponseEntity<Object> getManagersBySchool(@PathVariable String schoolId) {
        List<Manager> managers = managerRepository.findBySchoolId(schoolId);
        List<Map<String, Object>> result = buildManagerList(managers);
        return ResponseEntity.ok(Map.of("managers", result));
    }

    // ─── POST /managers/link-to-school ────────────────────────────────────────
    @PostMapping("/link-to-school")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECADM')")
    public ResponseEntity<Object> linkManagerToSchool(@RequestBody Map<String, String> body) {
        String userId = body.get("user_id");
        String schoolId = body.get("school_id");

        if (userId == null || schoolId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "user_id e school_id são obrigatórios"));
        }

        Optional<Manager> managerOpt = managerRepository.findByUserId(userId);
        if (managerOpt.isEmpty()) {
            // Create a manager record for this user if it doesn't exist
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Usuário não encontrado"));
            }
            User user = userOpt.get();
            Manager m = new Manager();
            m.setName(user.getName());
            m.setUser(user);
            m.setCity(user.getCity());
            m.setSchoolId(schoolId);
            managerRepository.save(m);
        } else {
            Manager m = managerOpt.get();
            m.setSchoolId(schoolId);
            managerRepository.save(m);
        }

        return ResponseEntity.ok(Map.of("message", "Vínculo realizado com sucesso"));
    }

    // ─── DELETE /managers/school-link/{userId} ─────────────────────────────────
    @DeleteMapping("/school-link/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECADM')")
    public ResponseEntity<Object> removeSchoolLink(@PathVariable String userId) {
        Optional<Manager> managerOpt = managerRepository.findByUserId(userId);
        if (managerOpt.isPresent()) {
            Manager m = managerOpt.get();
            m.setSchoolId(null);
            managerRepository.save(m);
        }
        return ResponseEntity.ok(Map.of("message", "Vínculo removido com sucesso"));
    }

    // ─── POST /managers (create user with manager role) ────────────────────────
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TECADM')")
    public ResponseEntity<Object> createManager(@RequestBody Map<String, String> body, Authentication auth) {
        User loggedUser = authService.getUserFromAuthentication(auth);

        User user = new User();
        user.setName(body.get("name"));
        user.setEmail(body.get("email"));
        user.setPasswordHash(passwordEncoder.encode(body.get("password")));
        user.setRole(RoleEnum.valueOf(body.get("role").toUpperCase()));
        user.setRegistration(body.get("registration"));

        if (body.get("birth_date") != null && !body.get("birth_date").isEmpty()) {
            user.setBirthDate(LocalDate.parse(body.get("birth_date")));
        }

        if (body.get("city_id") != null && !body.get("city_id").isEmpty()) {
            cityRepository.findById(body.get("city_id")).ifPresent(user::setCity);
        } else if (loggedUser != null && loggedUser.getCity() != null) {
            user.setCity(loggedUser.getCity());
        }

        user = userRepository.save(user);

        if (body.get("school_id") != null && !body.get("school_id").isEmpty()) {
            Manager m = new Manager();
            m.setName(user.getName());
            m.setUser(user);
            m.setCity(user.getCity()); // Link to municipality automatically
            m.setSchoolId(body.get("school_id"));
            managerRepository.save(m);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Usuário criado com sucesso",
                "id", user.getId()
        ));
    }

    // ─── Helper ────────────────────────────────────────────────────────────────
    private List<Map<String, Object>> buildManagerList(List<Manager> managers) {
        return managers.stream().map(m -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            Map<String, Object> managerMap = new LinkedHashMap<>();
            managerMap.put("id", m.getId());
            managerMap.put("name", m.getName());
            managerMap.put("registration", m.getRegistration());
            managerMap.put("birth_date", m.getBirthDate() != null ? m.getBirthDate().toString() : null);
            managerMap.put("profile_picture", m.getProfilePicture());
            managerMap.put("school_id", m.getSchoolId());
            entry.put("manager", managerMap);

            if (m.getUser() != null) {
                Map<String, Object> userMap = new LinkedHashMap<>();
                userMap.put("id", m.getUser().getId().toString());
                userMap.put("name", m.getUser().getName());
                userMap.put("email", m.getUser().getEmail());
                userMap.put("registration", m.getUser().getRegistration());
                userMap.put("role", m.getUser().getRole() != null ? m.getUser().getRole().name().toLowerCase() : null);
                entry.put("user", userMap);
            }

            return entry;
        }).collect(Collectors.toList());
    }
}
