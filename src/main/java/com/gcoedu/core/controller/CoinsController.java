package com.gcoedu.core.controller;

import com.gcoedu.core.domain.dto.tenant.gamification.CoinTransactionDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = {"/coins", "/api/coins"})
public class CoinsController {

    @GetMapping("/balance")
    public ResponseEntity<Map<String, Integer>> getBalance(@RequestParam(required = false) String student_id) {
        // Stub da fase 3
        return ResponseEntity.ok(Map.of("balance", 0));
    }

    @GetMapping("/transactions")
    public ResponseEntity<Map<String, List<CoinTransactionDTO>>> getTransactions(
            @RequestParam(required = false) String student_id,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset) {
        return ResponseEntity.ok(Map.of("transactions", List.of()));
    }

    @PostMapping("/admin/credit")
    public ResponseEntity<Map<String, Boolean>> credit(@RequestBody Map<String, Object> payload) {
        // Stub
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/admin/debit")
    public ResponseEntity<Map<String, Boolean>> debit(@RequestBody Map<String, Object> payload) {
        // Stub
        return ResponseEntity.ok(Map.of("success", true));
    }
}
