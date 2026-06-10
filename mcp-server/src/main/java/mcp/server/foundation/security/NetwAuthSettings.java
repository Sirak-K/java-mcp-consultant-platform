package mcp.server.foundation.security;

import java.util.Locale;
import java.util.Objects;

/**
 * Immutable auth settings for network-layer bearer-token authentication.
 *
 * <p>Rotation readiness: set {@code bearerTokenTransition} to the new token before
 * switching {@code bearerToken}. Both tokens are accepted while transition is active.
 * Clear {@code bearerTokenTransition} after all clients have rotated. The two tokens
 * must always differ when rotation is active.
 */
public record NetwAuthSettings(
    boolean enabled,
    String headerName,
    String scheme,
    String bearerToken,
    String bearerTokenTransition) {

  public NetwAuthSettings {
    headerName = requireNormalized(headerName, "headerName");
    scheme = requireNormalized(scheme, "scheme");
    bearerToken = bearerToken == null ? "" : bearerToken.trim();
    bearerTokenTransition = bearerTokenTransition == null ? "" : bearerTokenTransition.trim();

    if (enabled && bearerToken.isBlank()) {
      throw new IllegalArgumentException("Network auth is enabled but bearer token is blank");
    }

    if (!bearerTokenTransition.isBlank() && bearerTokenTransition.equals(bearerToken)) {
      throw new IllegalArgumentException(
          "bearer-token-transition must differ from bearer-token when rotation is active");
    }
  }

  public NetwAuthSettings(
      boolean enabled,
      String headerName,
      String scheme,
      String bearerToken) {

    this(enabled, headerName, scheme, bearerToken, "");
  }

  public boolean NetAuthHasTransitionToken() {
    return !bearerTokenTransition.isBlank();
  }

  public String NetAuthExpectedHeaderPrefix() {
    return scheme + " ";
  }

  public String NetAuthSchemeLowercase() {
    return scheme.toLowerCase(Locale.ROOT);
  }

  private static String requireNormalized(String rawValue, String fieldName) {
    Objects.requireNonNull(rawValue, fieldName);

    String normalized = rawValue.trim();
    if (normalized.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }

    return normalized;
  }
}
