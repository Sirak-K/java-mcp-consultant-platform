package mcp.server.foundation.security;

import mcp.server.foundation.security.request_binding.ReqsAuthBinding;
import mcp.server.foundation.security.request_binding.ReqsAuthBindingPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Objects;

@Service
@Profile("dev")
public final class DevAuthService {

  private final ReqsAuthBindingPolicy requestAuthBindingPolicy;
  private final String opsEmail;
  private final String opsPassword;

  public DevAuthService(
      ReqsAuthBindingPolicy requestAuthBindingPolicy,
      @Value("${mcp.app.dev-auth.ops.email}") String opsEmail,
      @Value("${mcp.app.dev-auth.ops.password}") String opsPassword) {

    this.requestAuthBindingPolicy = Objects.requireNonNull(requestAuthBindingPolicy, "requestAuthBindingPolicy");
    this.opsEmail = requireText(opsEmail, "opsEmail");
    this.opsPassword = requireText(opsPassword, "opsPassword");
  }

  public ReqsAuthBinding authenticate(String email, String password) {
    String normalizedEmail = normalizeEmail(email);
    String normalizedPassword = requireText(password, "password");

    if (matches(normalizedEmail, opsEmail) && normalizedPassword.equals(opsPassword)) {
      return requestAuthBindingPolicy.ReqsAuthBindingPolicyResolvePlatformOpsDefault(normalizedEmail);
    }
    throw new BadCredentialsException("Invalid dev credentials");
  }

  public ReqsAuthBinding devOpsBypassBinding() {
    return requestAuthBindingPolicy.ReqsAuthBindingPolicyResolvePlatformOpsDefault(
        normalizeEmail(opsEmail));
  }

  private boolean matches(String left, String right) {
    return left.equals(normalizeEmail(right));
  }

  private String normalizeEmail(String email) {
    return requireText(email, "email").toLowerCase(Locale.ROOT);
  }

  private static String requireText(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName);
    String normalized = value.trim();
    if (normalized.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return normalized;
  }
}
