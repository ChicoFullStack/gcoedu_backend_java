package com.gcoedu.core.controller;

import com.gcoedu.core.domain.dto.AuthRequest;
import com.gcoedu.core.domain.dto.AuthResponse;
import com.gcoedu.core.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = {"/api/auth", "/auth", "/api", ""})
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(value = {"/login", "/login/"})
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @GetMapping(value = {"/persist-user", "/persist-user/"})
    public ResponseEntity<AuthResponse> persistUser(org.springframework.security.core.Authentication authentication) {
        return ResponseEntity.ok(authService.persistUser(authentication.getName()));
    }

    @PostMapping(value = {"/logout", "/logout/"})
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok().build();
    }
}
