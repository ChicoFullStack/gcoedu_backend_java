package com.gcoedu.core.controller;

import com.gcoedu.core.domain.entity.publics.User;
import com.gcoedu.core.repository.publics.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = {"/users", "/api/users"})
public class UserController {

    private final UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final com.gcoedu.core.repository.publics.ManagerRepository managerRepository;
    private final com.gcoedu.core.repository.tenant.TeacherRepository teacherRepository;
    private final com.gcoedu.core.repository.tenant.StudentRepository studentRepository;
    private final com.gcoedu.core.repository.publics.UserSettingsRepository userSettingsRepository;
    private final com.gcoedu.core.repository.publics.UserQuickLinksRepository userQuickLinksRepository;
    private final com.gcoedu.core.service.tenant.BulkUploadService bulkUploadService;

    public UserController(UserRepository userRepository, 
                          org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
                          com.gcoedu.core.repository.publics.ManagerRepository managerRepository,
                          com.gcoedu.core.repository.tenant.TeacherRepository teacherRepository,
                          com.gcoedu.core.repository.tenant.StudentRepository studentRepository,
                          com.gcoedu.core.repository.publics.UserSettingsRepository userSettingsRepository,
                          com.gcoedu.core.repository.publics.UserQuickLinksRepository userQuickLinksRepository,
                          com.gcoedu.core.service.tenant.BulkUploadService bulkUploadService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.managerRepository = managerRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.userQuickLinksRepository = userQuickLinksRepository;
        this.bulkUploadService = bulkUploadService;
    }

    @GetMapping("/me/onboarding-status")
    public ResponseEntity<Map<String, Object>> getOnboardingStatus() {
        return ResponseEntity.ok(Map.of("needs_onboarding", false));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return ResponseEntity.ok(mapUserToResponse(user));
    }

    @org.springframework.web.bind.annotation.PostMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestBody Map<String, String> body) {
        String baseEmail = body.get("email");
        if (baseEmail == null || baseEmail.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        boolean exists = userRepository.findByEmail(baseEmail).isPresent();
        if (!exists) {
            return ResponseEntity.ok(Map.of("disponivel", true, "email", baseEmail));
        }

        // Generate suggestion
        String[] parts = baseEmail.split("@");
        String prefix = parts[0];
        String domain = parts.length > 1 ? "@" + parts[1] : "";
        
        String suggestion = baseEmail;
        int counter = 1;
        while (userRepository.findByEmail(suggestion).isPresent()) {
            suggestion = prefix + counter + domain;
            counter++;
        }
        
        return ResponseEntity.ok(Map.of("disponivel", false, "email", baseEmail, "email_sugerido", suggestion));
    }

    @org.springframework.web.bind.annotation.PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(org.springframework.security.core.Authentication authentication, @RequestBody Map<String, String> body) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.findByRegistration(email)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")));

        String currentPassword = body.get("current_password");
        String newPassword = body.get("new_password");

        if (currentPassword == null || newPassword == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senhas não informadas");
        }

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Senha atual incorreta"));
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable String id, @RequestBody Map<String, Object> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (body.containsKey("name") && body.get("name") != null) user.setName((String) body.get("name"));
        if (body.containsKey("phone")) user.setPhone((String) body.get("phone"));
        if (body.containsKey("address")) user.setAddress((String) body.get("address"));
        if (body.containsKey("gender")) user.setGender((String) body.get("gender"));
        if (body.containsKey("nationality")) user.setNationality((String) body.get("nationality"));
        if (body.containsKey("birth_date") && body.get("birth_date") != null) {
            String bd = (String) body.get("birth_date");
            if (!bd.isEmpty()) {
                user.setBirthDate(LocalDate.parse(bd));
            } else {
                user.setBirthDate(null);
            }
        } else if (body.containsKey("birth_date")) {
            user.setBirthDate(null);
        }

        if (body.containsKey("traits")) {
            try {
                // traits can be a list or null
                Object traits = body.get("traits");
                if (traits == null) {
                    user.setTraits(null);
                } else if (traits instanceof java.util.List) {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    user.setTraits(mapper.writeValueAsString(traits));
                }
            } catch (Exception e) {
                // ignore
            }
        }

        userRepository.save(user);

        return ResponseEntity.ok(mapUserToResponse(user));
    }

    @GetMapping("/user-settings/{id}")
    public ResponseEntity<Map<String, Object>> getUserSettings(@PathVariable String id) {
        return ResponseEntity.ok(Map.of("id", id));
    }

    @org.springframework.web.bind.annotation.PostMapping("/user-settings/{id}")
    public ResponseEntity<Map<String, Object>> updateUserSettings(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(body);
    }

    @GetMapping({"/managers", "/managers/me"})
    public ResponseEntity<Object> getManagers() {
        return ResponseEntity.ok(java.util.Collections.emptyList());
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'TECADM', 'MANAGER')")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // 1. Delete associated manager, settings, and quick links records
        managerRepository.findByUserId(id).ifPresent(managerRepository::delete);
        userSettingsRepository.findByUserId(id).ifPresent(userSettingsRepository::delete);
        java.util.List<com.gcoedu.core.domain.entity.publics.UserQuickLinks> quickLinks = userQuickLinksRepository.findByUserId(id);
        if (quickLinks != null && !quickLinks.isEmpty()) {
            userQuickLinksRepository.deleteAll(quickLinks);
        }

        // 2. Switch context to delete tenant-specific teacher / student records
        String originalTenant = com.gcoedu.core.config.tenant.TenantContext.getCurrentTenant();
        String targetSchema = "public";
        if (user.getCity() != null) {
            targetSchema = "city_" + user.getCity().getId().replace("-", "_");
        }
        
        try {
            com.gcoedu.core.config.tenant.TenantContext.setCurrentTenant(targetSchema);
            teacherRepository.findByUserId(id).ifPresent(teacherRepository::delete);
            studentRepository.findByUserId(id).ifPresent(studentRepository::delete);
        } finally {
            com.gcoedu.core.config.tenant.TenantContext.setCurrentTenant(originalTenant);
        }

        // 3. Delete the user
        userRepository.delete(user);
        return ResponseEntity.noContent().build();
    }

    @org.springframework.web.bind.annotation.PostMapping("/bulk-upload-students")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'TECADM', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> bulkUploadStudents(
            @org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @org.springframework.web.bind.annotation.RequestParam(value = "class_id", required = false) String classId) {
        Map<String, Object> result = bulkUploadService.uploadStudents(file, classId);
        int successes = (int) ((Map<?, ?>) result.get("resumo")).get("sucessos");
        if (successes > 0) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    private Map<String, Object> mapUserToResponse(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("name", user.getName());
        userMap.put("email", user.getEmail());
        if (user.getRole() != null) userMap.put("role", user.getRole().name().toLowerCase());
        
        String tenantId = user.getCity() != null ? user.getCity().getId() : "public";
        userMap.put("tenant_id", tenantId);
        userMap.put("city_id", tenantId);
        userMap.put("registration", user.getRegistration());
        userMap.put("phone", user.getPhone());
        userMap.put("address", user.getAddress());
        userMap.put("gender", user.getGender());
        userMap.put("nationality", user.getNationality());
        userMap.put("birth_date", user.getBirthDate() != null ? user.getBirthDate().toString() : null);
        
        try {
            if (user.getTraits() != null) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                userMap.put("traits", mapper.readValue(user.getTraits(), java.util.List.class));
            } else {
                userMap.put("traits", java.util.Collections.emptyList());
            }
        } catch (Exception e) {
            userMap.put("traits", java.util.Collections.emptyList());
        }

        return userMap;
    }
}
