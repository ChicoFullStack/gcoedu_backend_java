package com.gcoedu.core.config.tenant;

import com.gcoedu.core.config.security.JwtUtils;
import com.gcoedu.core.repository.publics.CityRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantInterceptorTest {

    private final JwtUtils jwtUtils = mock(JwtUtils.class);
    private final CityRepository cityRepository = mock(CityRepository.class);
    private final TenantInterceptor interceptor =
            new TenantInterceptor(jwtUtils, cityRepository);

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void rejectsNonAdminTokenWithoutTenantInsteadOfUsingHeaderFallback() throws Exception {
        HttpServletRequest request = authenticatedRequest("TECADM", null);
        when(request.getHeader("x-tenant-id"))
                .thenReturn("11111111-1111-1111-1111-111111111111");
        HttpServletResponse response = responseWithWriter();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        assertThat(TenantContext.getCurrentTenant()).isEqualTo(TenantContext.DEFAULT_TENANT);
    }

    @Test
    void pinsNonAdminToJwtTenantAndIgnoresTenantHeaders() {
        HttpServletRequest request = authenticatedRequest(
                "DIRETOR", "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        when(request.getHeader("x-tenant-id"))
                .thenReturn("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        HttpServletResponse response = mock(HttpServletResponse.class);

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
        assertThat(TenantContext.getCurrentTenant())
                .isEqualTo("city_aaaaaaaa_aaaa_aaaa_aaaa_aaaaaaaaaaaa");
    }

    @Test
    void allowsAdminToSelectAValidTenant() {
        HttpServletRequest request = authenticatedRequest("ADMIN", null);
        when(request.getHeader("x-tenant-id"))
                .thenReturn("cccccccc-cccc-cccc-cccc-cccccccccccc");
        HttpServletResponse response = mock(HttpServletResponse.class);

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
        assertThat(TenantContext.getCurrentTenant())
                .isEqualTo("city_cccccccc_cccc_cccc_cccc_cccccccccccc");
    }

    @Test
    void rejectsInvalidPublicTenantIdentifier() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("x-tenant-id")).thenReturn("city_a;set schema public");
        HttpServletResponse response = responseWithWriter();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    private HttpServletRequest authenticatedRequest(String role, String tenant) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Claims claims = mock(Claims.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtils.validateToken("token")).thenReturn(true);
        when(jwtUtils.getClaims("token")).thenReturn(claims);
        when(claims.get("role", String.class)).thenReturn(role);
        when(claims.get("tenant", String.class)).thenReturn(tenant);
        return request;
    }

    private HttpServletResponse responseWithWriter() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        return response;
    }
}
