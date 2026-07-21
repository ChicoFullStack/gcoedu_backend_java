package com.gcoedu.core.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(org.springframework.security.config.Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/auth/**", "/login", "/login/", "/api/login", "/api/login/", "/swagger-ui/**", "/v3/api-docs/**", "/swagger.yaml", "/subdomain/**", "/api/subdomain/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/subjects", "/subjects/", "/api/subjects", "/api/subjects/").hasAnyRole("ADMIN", "TECADM")
                .requestMatchers(HttpMethod.PUT, "/subjects/**", "/api/subjects/**").hasAnyRole("ADMIN", "TECADM")
                .requestMatchers(HttpMethod.DELETE, "/subjects/**", "/api/subjects/**").hasAnyRole("ADMIN", "TECADM")
                .requestMatchers(HttpMethod.GET, "/skills", "/skills/", "/skills/**", "/api/skills", "/api/skills/", "/api/skills/**")
                    .hasAnyRole("ADMIN", "TECADM", "DIRETOR", "COORDENADOR", "PROFESSOR")
                .requestMatchers(HttpMethod.POST, "/skills/batch", "/api/skills/batch")
                    .hasAnyRole("ADMIN", "TECADM")
                .requestMatchers(HttpMethod.POST, "/skills", "/skills/", "/api/skills", "/api/skills/")
                    .hasAnyRole("ADMIN", "TECADM", "DIRETOR", "COORDENADOR", "PROFESSOR")
                .requestMatchers(HttpMethod.DELETE, "/skills/**", "/api/skills/**").hasRole("ADMIN")
                .requestMatchers(
                        "/questions/**",
                        "/api/questions/**",
                        "/api/v1/tenant/questions/**"
                    )
                    .hasAnyRole("ADMIN", "TECADM", "DIRETOR", "COORDENADOR", "PROFESSOR")
                .requestMatchers(HttpMethod.GET,
                        "/dashboard/analise-sistema", "/api/dashboard/analise-sistema",
                        "/dashboard/avisos/quantidade", "/api/dashboard/avisos/quantidade")
                    .hasAnyRole("ADMIN", "TECADM")
                .requestMatchers(HttpMethod.GET, "/dashboard/admin", "/api/dashboard/admin")
                    .hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,
                        "/dashboard/ranking-turmas", "/api/dashboard/ranking-turmas")
                    .hasAnyRole("ADMIN", "TECADM", "DIRETOR", "COORDENADOR")
                .requestMatchers(HttpMethod.GET,
                        "/dashboard/ranking-alunos", "/api/dashboard/ranking-alunos")
                    .hasAnyRole("ADMIN", "TECADM", "DIRETOR", "COORDENADOR", "PROFESSOR", "ALUNO")
                .requestMatchers(HttpMethod.GET,
                        "/dashboard/questoes", "/api/dashboard/questoes",
                        "/dashboard/avaliacoes-recentes", "/api/dashboard/avaliacoes-recentes")
                    .hasAnyRole("ADMIN", "TECADM", "DIRETOR", "COORDENADOR", "PROFESSOR")
                .requestMatchers(HttpMethod.GET,
                        "/dashboard/comprehensive-stats", "/api/dashboard/comprehensive-stats",
                        "/evaluations/stats", "/api/evaluations/stats")
                    .hasAnyRole("ADMIN", "TECADM", "DIRETOR", "COORDENADOR", "PROFESSOR")
                .requestMatchers(HttpMethod.GET,
                        "/certificates/quantidade", "/api/certificates/quantidade")
                    .hasAnyRole("ADMIN", "TECADM", "DIRETOR", "COORDENADOR")
                .requestMatchers(HttpMethod.POST,
                        "/calendar/events", "/api/calendar/events",
                        "/calendar/events/*/publish", "/api/calendar/events/*/publish",
                        "/calendar/events/*/resources/file", "/api/calendar/events/*/resources/file")
                    .hasAnyRole("ADMIN", "TECADM", "DIRETOR", "COORDENADOR", "PROFESSOR")
                .requestMatchers(HttpMethod.PUT,
                        "/calendar/events/*", "/api/calendar/events/*")
                    .hasAnyRole("ADMIN", "TECADM", "DIRETOR", "COORDENADOR", "PROFESSOR")
                .requestMatchers(HttpMethod.DELETE,
                        "/calendar/events/**", "/api/calendar/events/**")
                    .hasAnyRole("ADMIN", "TECADM", "DIRETOR", "COORDENADOR", "PROFESSOR")
                .requestMatchers(HttpMethod.GET,
                        "/calendar/targets/me", "/api/calendar/targets/me")
                    .hasAnyRole("ADMIN", "TECADM", "DIRETOR", "COORDENADOR", "PROFESSOR")
                .requestMatchers(HttpMethod.GET,
                        "/play-tv/videos", "/play-tv/videos/",
                        "/play-tv/videos/**",
                        "/api/play-tv/videos", "/api/play-tv/videos/",
                        "/api/play-tv/videos/**")
                    .hasAnyRole("ADMIN", "TECADM", "DIRETOR", "COORDENADOR", "PROFESSOR", "ALUNO")
                .requestMatchers(HttpMethod.POST,
                        "/play-tv/videos", "/api/play-tv/videos",
                        "/play-tv/videos/*/resources/file",
                        "/api/play-tv/videos/*/resources/file")
                    .hasAnyRole("ADMIN", "TECADM", "DIRETOR", "COORDENADOR", "PROFESSOR")
                .requestMatchers(HttpMethod.PUT,
                        "/play-tv/videos/*", "/api/play-tv/videos/*")
                    .hasAnyRole("ADMIN", "TECADM", "DIRETOR", "COORDENADOR", "PROFESSOR")
                .requestMatchers(HttpMethod.DELETE,
                        "/play-tv/videos/**", "/api/play-tv/videos/**")
                    .hasAnyRole("ADMIN", "TECADM", "DIRETOR", "COORDENADOR", "PROFESSOR")
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"Authentication is required to access this resource\"}");
                })
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new WerkzeugPasswordEncoder();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOriginPatterns(java.util.List.of("*"));
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.List.of("*"));
        configuration.setAllowCredentials(false);
        
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
