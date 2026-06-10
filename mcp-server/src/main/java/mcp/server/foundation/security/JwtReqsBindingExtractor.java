package mcp.server.foundation.security;

import mcp.server.foundation.security.request_binding.ReqsAuthBinding;
import mcp.server.foundation.security.request_binding.ReqsAuthBindingPolicy;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Extracts JWT claims from SecurityContext and builds a TENANT_BOUND ReqsAuthBinding.
 * Only active in prod profile where Spring Security JWT resource server is configured.
 */
@Component
@Profile("prod")
public class JwtReqsBindingExtractor {

  private final ReqsAuthBindingPolicy requestAuthBindingPolicy;

  public JwtReqsBindingExtractor(
      ReqsAuthBindingPolicy requestAuthBindingPolicy) {
    this.requestAuthBindingPolicy = Objects.requireNonNull(requestAuthBindingPolicy, "requestAuthBindingPolicy");
  }

  /**
   * Reads JWT Authentication from SecurityContext and builds an MCP-direct platform binding.
   * Returns null if no JWT Authentication is present (fallback to default binding).
   * Throws TranspAuthExcep if JWT is present but the subject is missing.
   */
  public ReqsAuthBinding JwtExtractBinding() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
      return null;
    }

    Jwt jwt = jwtAuth.getToken();
    String sub = jwt.getSubject();

    if (sub == null || sub.isBlank()) {
      throw new TranspAuthExcep(
          "JWT missing required claim: sub",
          TranspAuthFailureReason.INVALID_TOKEN);
    }

    return requestAuthBindingPolicy.ReqsAuthBindingPolicyResolveJwtMcpDefault(sub);
  }
}
