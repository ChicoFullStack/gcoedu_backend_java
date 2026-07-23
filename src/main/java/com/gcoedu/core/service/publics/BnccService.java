package com.gcoedu.core.service.publics;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
public class BnccService {

    @Value("${bncc.api.url}")
    private String apiUrl;

    @Value("${bncc.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public Object getHabilidades(Map<String, String> queryParams) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiUrl + "/habilidades");
        if (queryParams != null) {
            queryParams.forEach(builder::queryParam);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    Object.class
            );
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao comunicar com a BNCC API: " + e.getMessage(), e);
        }
    }
    
    public Object getHabilidadeByCodigo(String codigo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                    apiUrl + "/habilidades/" + codigo,
                    HttpMethod.GET,
                    entity,
                    Object.class
            );
            return response.getBody();
        } catch (Exception e) {
             throw new RuntimeException("Erro ao comunicar com a BNCC API: " + e.getMessage(), e);
        }
    }
    
    public Object searchHabilidades(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        HttpEntity<?> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                    apiUrl + "/busca-semantica",
                    HttpMethod.POST,
                    entity,
                    Object.class
            );
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao comunicar com a BNCC API: " + e.getMessage(), e);
        }
    }
}
