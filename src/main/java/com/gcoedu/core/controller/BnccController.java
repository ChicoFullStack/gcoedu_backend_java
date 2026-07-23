package com.gcoedu.core.controller;

import com.gcoedu.core.service.publics.BnccService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/bncc")
@RequiredArgsConstructor
public class BnccController {

    private static final String QUESTION_AUTHOR_ROLES =
            "hasAnyRole('ADMIN', 'TECADM', 'DIRETOR', 'COORDENADOR', 'PROFESSOR')";

    private final BnccService bnccService;

    @GetMapping("/habilidades")
    @PreAuthorize(QUESTION_AUTHOR_ROLES)
    public ResponseEntity<Object> getHabilidades(@RequestParam Map<String, String> queryParams) {
        return ResponseEntity.ok(bnccService.getHabilidades(queryParams));
    }

    @GetMapping("/habilidades/{codigo}")
    @PreAuthorize(QUESTION_AUTHOR_ROLES)
    public ResponseEntity<Object> getHabilidadeByCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(bnccService.getHabilidadeByCodigo(codigo));
    }

    @PostMapping("/busca-semantica")
    @PreAuthorize(QUESTION_AUTHOR_ROLES)
    public ResponseEntity<Object> searchHabilidades(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(bnccService.searchHabilidades(body));
    }
}
