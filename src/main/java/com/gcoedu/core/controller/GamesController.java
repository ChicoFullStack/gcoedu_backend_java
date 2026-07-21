package com.gcoedu.core.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping(value = {"/games", "/api/games"})
public class GamesController {

    @GetMapping
    public ResponseEntity<List<Object>> getGames() {
        // Stub for games list
        return ResponseEntity.ok(Collections.emptyList());
    }
}
