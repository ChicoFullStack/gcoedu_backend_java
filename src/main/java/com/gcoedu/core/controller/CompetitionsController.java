package com.gcoedu.core.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = {"/competitions", "/api/competitions"})
public class CompetitionsController {

    @GetMapping("/ranking")
    public ResponseEntity<Map<String, List<Object>>> getRanking() {
        // Stub da fase 3
        return ResponseEntity.ok(Map.of("ranking", List.of()));
    }
}
