package com.gcoedu.core.service;

import com.gcoedu.core.domain.dto.AuthRequest;
import com.gcoedu.core.domain.dto.AuthResponse;
import com.gcoedu.core.domain.entity.publics.User;
import com.gcoedu.core.repository.publics.UserRepository;
import com.gcoedu.core.config.security.JwtUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    public AuthResponse authenticate(AuthRequest request) {
        String id = request.getResolvedIdentifier();
        if (id == null || id.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Identificador não informado");
        }

        User user = userRepository.findByEmail(id)
                .orElseGet(() -> userRepository.findByRegistration(id)
                        .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Credenciais inválidas")));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
        }

        String tenantId = user.getCity() != null ? user.getCity().getId() : "public";
        String token = jwtUtils.generateToken(user.getEmail(), user.getRole().name(), tenantId);

        java.util.Map<String, Object> userMap = new java.util.HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("name", user.getName());
        userMap.put("email", user.getEmail());
        userMap.put("role", user.getRole().name().toLowerCase());
        userMap.put("tenant_id", tenantId);
        userMap.put("city_id", tenantId);
        userMap.put("registration", user.getRegistration());
        userMap.put("phone", user.getPhone());
        userMap.put("address", user.getAddress());
        userMap.put("gender", user.getGender());
        userMap.put("nationality", user.getNationality());
        userMap.put("birth_date", user.getBirthDate() != null ? user.getBirthDate().toString() : null);

        return AuthResponse.builder()
                .token(token)
                .user(userMap)
                .build();
    }

    public AuthResponse persistUser(String id) {
        if (id == null || id.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Identificador não informado");
        }

        User user = userRepository.findByEmail(id)
                .orElseGet(() -> userRepository.findByRegistration(id)
                        .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Usuário não encontrado")));

        String tenantId = user.getCity() != null ? user.getCity().getId() : "public";

        java.util.Map<String, Object> userMap = new java.util.HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("name", user.getName());
        userMap.put("email", user.getEmail());
        userMap.put("role", user.getRole().name().toLowerCase());
        userMap.put("tenant_id", tenantId);
        userMap.put("city_id", tenantId);
        userMap.put("registration", user.getRegistration());
        userMap.put("phone", user.getPhone());
        userMap.put("address", user.getAddress());
        userMap.put("gender", user.getGender());
        userMap.put("nationality", user.getNationality());
        userMap.put("birth_date", user.getBirthDate() != null ? user.getBirthDate().toString() : null);

        return AuthResponse.builder()
                .token(null) // persist doesn't need to return token unless refreshed
                .user(userMap)
                .build();
    }

    public User getUserFromAuthentication(org.springframework.security.core.Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        String email = auth.getName();
        return userRepository.findByEmail(email).orElse(null);
    }
}

