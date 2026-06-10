package mcp.server.foundation.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security JWT Resource Server configuration for prod profile.
 * Validates Keycloak JWTs against JWKS endpoint (configured via spring.security.oauth2.resourceserver.jwt.issuer-uri).
 * Stateless - no HTTP session created.
 */
@Configuration
@EnableWebSecurity
@Profile("prod")
public class JwtAuthCfg {

  @Bean
  public SecurityFilterChain JwtAuthSecurityFilterChain(HttpSecurity http) throws Exception {
    http
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(authz -> authz
            .requestMatchers("/actuator/health", "/actuator/info").permitAll()
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> {}));

    return http.build();
  }
}
