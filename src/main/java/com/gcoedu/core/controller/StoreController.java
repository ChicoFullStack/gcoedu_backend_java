package com.gcoedu.core.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = {"/store", "/api/store"})
public class StoreController {

    @GetMapping("/items")
    public ResponseEntity<Map<String, List<Object>>> getStoreItems() {
        // Stub da fase 3
        return ResponseEntity.ok(Map.of("items", List.of()));
    }

    @PostMapping("/purchase")
    public ResponseEntity<Map<String, Boolean>> purchaseItem(@RequestBody Map<String, Object> payload) {
        // Stub da fase 3
        return ResponseEntity.ok(Map.of("success", true));
    }
}
