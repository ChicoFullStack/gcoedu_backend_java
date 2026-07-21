package com.gcoedu.core.controller;

import com.gcoedu.core.domain.entity.publics.City;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.gcoedu.core.repository.publics.CityRepository;
import com.gcoedu.core.repository.publics.UserRepository;
import com.gcoedu.core.domain.entity.publics.User;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = {"/city", "/api/city"})
@RequiredArgsConstructor
public class CityController {

    private final CityRepository cityRepository;
    private final UserRepository userRepository;

    @GetMapping({"", "/"})
    public ResponseEntity<List<City>> getAllCities() {
        List<City> cities = cityRepository.findAll();
        return ResponseEntity.ok(cities);
    }

    @GetMapping("/{id}")
    public ResponseEntity<City> getCityById(@PathVariable String id) {
        return cityRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/users")
    public ResponseEntity<Map<String, Object>> getUsersByCity(@PathVariable String id) {
        List<User> users = userRepository.findByCityId(id);
        List<Map<String, Object>> mappedUsers = users.stream()
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "name", u.getName() != null ? u.getName() : "",
                        "email", u.getEmail() != null ? u.getEmail() : "",
                        "role", u.getRole() != null ? u.getRole().name() : ""
                ))
                .toList();
        return ResponseEntity.ok(Map.of("users", mappedUsers));
    }

    @GetMapping("/states")
    public ResponseEntity<List<Object>> getStates() {
        return ResponseEntity.ok(java.util.Collections.emptyList());
    }
}
