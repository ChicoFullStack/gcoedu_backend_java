package com.gcoedu.core.controller;

import com.gcoedu.core.domain.dto.tenant.DashboardDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = {"/certificates", "/api/certificates"})
public class CertificatesController {

    @GetMapping("/quantidade")
    public ResponseEntity<DashboardDTO.Quantity> getCertificatesQuantidade() {
        return ResponseEntity.ok(new DashboardDTO.Quantity(0));
    }

    @GetMapping("/evaluations")
    public ResponseEntity<java.util.Map<String, java.util.List<Object>>> getEvaluations() {
        return ResponseEntity.ok(java.util.Map.of("data", java.util.Collections.emptyList()));
    }
}
