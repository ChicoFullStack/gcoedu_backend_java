package com.gcoedu.core.config.tenant;

import com.gcoedu.core.config.security.JwtUtils;
import com.gcoedu.core.domain.entity.publics.City;
import com.gcoedu.core.repository.publics.CityRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final String TENANT_HEADER = "x-tenant-id";
    private static final String CITY_HEADER = "x-city-id";
    private static final String SLUG_HEADER = "x-city-slug";

    private final JwtUtils jwtUtils;
    private final CityRepository cityRepository;

    public TenantInterceptor(JwtUtils jwtUtils, CityRepository cityRepository) {
        this.jwtUtils = jwtUtils;
        this.cityRepository = cityRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        TenantContext.clear();
        String resolvedTenant = null;
        String token = parseJwt(request);
        boolean authenticatedToken = token != null && jwtUtils.validateToken(token);

        if (authenticatedToken) {
            try {
                Claims claims = jwtUtils.getClaims(token);
                String role = claims.get("role", String.class);
                String userTenantId = claims.get("tenant", String.class);

                if ("ADMIN".equalsIgnoreCase(role)) {
                    resolvedTenant = resolveAdminTenant(request);
                } else {
                    // Non-admin roles are always pinned to the tenant signed into their JWT.
                    resolvedTenant = userTenantId;
                    if (resolvedTenant == null || resolvedTenant.isBlank()) {
                        return reject(response, HttpServletResponse.SC_FORBIDDEN,
                                "Token sem tenant para o perfil autenticado");
                    }
                }
            } catch (Exception exception) {
                log.warn("Falha ao resolver contexto de segurança do tenant: {}",
                        exception.getMessage());
                return reject(response, HttpServletResponse.SC_FORBIDDEN,
                        "Não foi possível resolver o tenant do usuário");
            }
        }

        // Public endpoints (including login) may resolve a tenant from explicit headers.
        // This fallback never runs for a valid non-admin JWT.
        if (!authenticatedToken && (resolvedTenant == null || resolvedTenant.isBlank())) {
            resolvedTenant = firstNonBlank(
                    request.getHeader(TENANT_HEADER),
                    request.getHeader(CITY_HEADER));
        }

        if (!isValidTenantIdentifier(resolvedTenant)) {
            return reject(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Identificador de tenant inválido");
        }

        TenantContext.setCurrentTenant(convertCityIdToSchemaName(resolvedTenant));
        return true;
    }

    private String resolveAdminTenant(HttpServletRequest request) {
        String tenant = firstNonBlank(
                request.getHeader(TENANT_HEADER),
                request.getHeader(CITY_HEADER));
        if (tenant != null) {
            return tenant;
        }

        String slugHeader = request.getHeader(SLUG_HEADER);
        if (slugHeader != null && !slugHeader.isBlank()) {
            Optional<City> city = cityRepository.findBySlug(slugHeader.trim().toLowerCase());
            if (city.isPresent()) {
                return city.get().getId();
            }
        }

        String slug = extractSubdomainSlug(request.getHeader("Host"));
        if (slug == null) {
            return null;
        }
        return cityRepository.findBySlug(slug).map(City::getId).orElse(null);
    }

    private String parseJwt(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        String queryToken = request.getParameter("access_token");
        return queryToken == null || queryToken.isBlank() ? null : queryToken;
    }

    private String convertCityIdToSchemaName(String cityId) {
        if (cityId == null || cityId.isBlank() || cityId.equalsIgnoreCase("public")) {
            return TenantContext.DEFAULT_TENANT;
        }
        if (cityId.startsWith("city_")) {
            return cityId;
        }
        return "city_" + cityId.replace("-", "_");
    }

    private boolean isValidTenantIdentifier(String tenant) {
        if (tenant == null || tenant.isBlank() || tenant.equalsIgnoreCase("public")) {
            return true;
        }
        if (tenant.matches("^city_[a-zA-Z0-9_]+$")) {
            return true;
        }
        try {
            UUID.fromString(tenant);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second != null && !second.isBlank() ? second : null;
    }

    private boolean reject(HttpServletResponse response, int status, String message) {
        TenantContext.clear();
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        try {
            response.getWriter().write(
                    "{\"status\":" + status
                            + ",\"error\":\"Tenant context\",\"message\":\""
                            + message + "\"}");
        } catch (IOException exception) {
            log.warn("Falha ao escrever resposta de tenant inválido: {}",
                    exception.getMessage());
        }
        return false;
    }

    private String extractSubdomainSlug(String host) {
        if (host == null || host.isBlank()) {
            return null;
        }
        if (host.contains("://")) {
            host = host.split("://", 2)[1];
        }
        host = host.split(":")[0].trim().toLowerCase();

        Set<String> ignoredHosts = Set.of(
                "gcoedu.com.br", "www.gcoedu.com.br", "api.gcoedu.com.br",
                "files.gcoedu.com.br", "localhost", "127.0.0.1", "gcoedu.com");
        if (ignoredHosts.contains(host)) {
            return null;
        }
        String[] parts = host.split("\\.");
        if (parts.length >= 2 && parts[0].matches("^[a-z0-9-]+$")) {
            return parts[0];
        }
        return null;
    }

    @Override
    public void postHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            ModelAndView modelAndView) {
        TenantContext.clear();
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception) {
        TenantContext.clear();
    }
}
