package mcp.server.foundation.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mcp.server.foundation.security.enforcement.TenantCtxHolder;
import mcp.server.foundation.security.request_binding.ReqsAuthBinding;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Configuration
@Profile("dev")
public class DevSecurityCfg {

  private final ObjectMapper objectMapper;
  private final String allowedOrigin;
  private final DevAuthService devAuthService;
  private final boolean opsBypassEnabled;

  public DevSecurityCfg(
      ObjectMapper objectMapper,
      DevAuthService devAuthService,
      @Value("${mcp.app.dev-auth.allowed-origin}") String allowedOrigin,
      @Value("${mcp.app.dev-auth.ops-bypass.enabled:false}") boolean opsBypassEnabled) {

    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    this.allowedOrigin = Objects.requireNonNull(allowedOrigin, "allowedOrigin");
    this.devAuthService = Objects.requireNonNull(devAuthService, "devAuthService");
    this.opsBypassEnabled = opsBypassEnabled;
  }

  @Bean
  public SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .requestCache(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
            .requestMatchers("/api/auth/me", "/api/auth/logout").authenticated()
            .requestMatchers("/api/ops/**").hasRole("OPS")
            .anyRequest().permitAll())
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, exception) ->
                writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required"))
            .accessDeniedHandler((request, response, exception) ->
                writeJsonError(response, HttpServletResponse.SC_FORBIDDEN, "Access denied")))
        .addFilterAfter(
            new DevOpsBypassAndTenantCtxFilter(devAuthService, opsBypassEnabled),
            SecurityContextHolderFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource devCorsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowCredentials(true);
    config.setAllowedOrigins(List.of(allowedOrigin));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Content-Type", "Authorization", "X-Requested-With"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    source.registerCorsConfiguration("/actuator/**", config);
    return source;
  }

  private void writeJsonError(HttpServletResponse response, int status, String message) throws IOException {
    response.setStatus(status);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(response.getWriter(), Map.of("message", message));
  }

  private static final class DevOpsBypassAndTenantCtxFilter extends OncePerRequestFilter {

    private static final PathPatternRequestMatcher.Builder PATH_MATCHER =
        PathPatternRequestMatcher.withDefaults();
    private static final RequestMatcher API_MATCHER = PATH_MATCHER.matcher("/api/**");
    private static final RequestMatcher OPS_API_MATCHER = PATH_MATCHER.matcher("/api/ops/**");
    private static final RequestMatcher AUTH_ME_MATCHER = PATH_MATCHER.matcher("/api/auth/me");
    private static final RequestMatcher AUTH_LOGOUT_MATCHER = PATH_MATCHER.matcher("/api/auth/logout");

    private final DevAuthService devAuthService;
    private final boolean opsBypassEnabled;

    private DevOpsBypassAndTenantCtxFilter(
        DevAuthService devAuthService,
        boolean opsBypassEnabled) {

      this.devAuthService = Objects.requireNonNull(devAuthService, "devAuthService");
      this.opsBypassEnabled = opsBypassEnabled;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
      return !API_MATCHER.matches(request);
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {

      boolean bypassAuthenticated = false;
      try {
        if (shouldUseOpsBypass(request) && !hasRequestAuthBinding()) {
          ReqsAuthBinding requestAuthBinding = devAuthService.devOpsBypassBinding();
          UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
              requestAuthBinding,
              null,
              List.of(new SimpleGrantedAuthority(requestAuthBinding.authority())));
          SecurityContextHolder.getContext().setAuthentication(authentication);
          bypassAuthenticated = true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
            && authentication.isAuthenticated()
            && authentication.getPrincipal() instanceof ReqsAuthBinding requestAuthBinding) {
          TenantCtxHolder.setFromReqsAuthBinding(requestAuthBinding);
        }
        filterChain.doFilter(request, response);
      } finally {
        TenantCtxHolder.clear();
        if (bypassAuthenticated) {
          SecurityContextHolder.clearContext();
        }
      }
    }

    private boolean shouldUseOpsBypass(HttpServletRequest request) {
      return opsBypassEnabled
          && (OPS_API_MATCHER.matches(request)
              || AUTH_ME_MATCHER.matches(request)
              || AUTH_LOGOUT_MATCHER.matches(request));
    }

    private boolean hasRequestAuthBinding() {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      return authentication != null
          && authentication.isAuthenticated()
          && authentication.getPrincipal() instanceof ReqsAuthBinding;
    }
  }
}
