package com.dhruvsharma.feed.postservice.config;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the Post Service.
 *
 * This is the second layer of defense (defense in depth).
 * Even though the API Gateway validates tokens first,
 * each microservice independently validates JWT tokens
 * to ensure security even if the gateway is bypassed.
 *
 * Roles are extracted from Keycloak's realm_access.roles claim
 * and mapped to Spring Security authorities with ROLE_ prefix.
 *
 * Authorization rules:
 * - POST /api/v1/posts  -> requires feed_user role
 * - GET  /api/v1/posts  -> requires feed_user role
 * - DELETE endpoints     -> requires feed_moderator or feed_admin role
 * - Actuator endpoints   -> public (for K8s health checks)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // Actuator endpoints for K8s probes
                .requestMatchers("/actuator/**").permitAll()
                // Post creation requires feed_user role
                .requestMatchers(HttpMethod.POST, "/api/v1/posts/**").hasRole("feed_user")
                // Reading posts requires feed_user role
                .requestMatchers(HttpMethod.GET, "/api/v1/posts/**").hasRole("feed_user")
                // Delete requires moderator or admin
                .requestMatchers(HttpMethod.DELETE, "/api/v1/posts/**").hasAnyRole("feed_moderator", "feed_admin")
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtAuthenticationConverter()))
            );

        return http.build();
    }

    /**
     * Configures JWT authentication converter to extract Keycloak realm roles.
     */
    @Bean
    public JwtAuthenticationConverter keycloakJwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return converter;
    }

    /**
     * Extracts realm roles from Keycloak JWT token.
     *
     * Keycloak JWT structure:
     * {
     *   "realm_access": {
     *     "roles": ["feed_user", "feed_admin"]
     *   }
     * }
     *
     * Maps to: ROLE_feed_user, ROLE_feed_admin
     */
    static class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

        @Override
        @SuppressWarnings("unchecked")
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess == null || !realmAccess.containsKey("roles")) {
                return Collections.emptyList();
            }

            List<String> roles = (List<String>) realmAccess.get("roles");
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        }
    }
}
