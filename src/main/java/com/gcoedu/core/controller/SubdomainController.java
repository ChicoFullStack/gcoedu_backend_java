package com.gcoedu.core.controller;

import com.gcoedu.core.repository.publics.CityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(value = {"/subdomain", "/api/subdomain"})
@RequiredArgsConstructor
public class SubdomainController {

    private final CityRepository cityRepository;

    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkSubdomain(@RequestParam String subdomain) {
        boolean exists = cityRepository.findBySlug(subdomain).isPresent();
        return ResponseEntity.ok(Map.of("exists", exists));
    }
}
