package mcp.server.foundation.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Objects;

public final class NetwAuthService {

  private final NetwAuthSettings settings;

  public NetwAuthService(NetwAuthSettings settings) {
    this.settings = Objects.requireNonNull(settings, "settings");
  }

  public NetwAuthSettings NetAuthSettings() {
    return settings;
  }

  public boolean NetAuthIsEnabled() {
    return settings.enabled();
  }

  public String NetAuthHeaderName() {
    return settings.headerName();
  }

  public String NetAuthWwwAuthenticateHeaderValue() {
    return settings.scheme();
  }

  public void NetAuthAssertAuthorized(String authorizationHeaderValue, String transportName) {

    Objects.requireNonNull(transportName, "transportName");

    if (!settings.enabled()) {
      return;
    }

    if (authorizationHeaderValue == null || authorizationHeaderValue.isBlank()) {
      throw new TranspAuthExcep(
          "Missing " + settings.headerName() + " header for transport " + transportName,
          TranspAuthFailureReason.MISSING_HEADER);
    }

    String normalizedHeader = authorizationHeaderValue.trim();
    int delimiter = normalizedHeader.indexOf(' ');

    if (delimiter <= 0 || delimiter == normalizedHeader.length() - 1) {
      throw new TranspAuthExcep(
          "Malformed " + settings.headerName() + " header for transport " + transportName,
          TranspAuthFailureReason.MALFORMED_HEADER);
    }

    String scheme = normalizedHeader.substring(0, delimiter).trim();
    String token = normalizedHeader.substring(delimiter + 1).trim();

    if (!scheme.toLowerCase(Locale.ROOT).equals(settings.NetAuthSchemeLowercase())) {
      throw new TranspAuthExcep(
          "Unsupported auth scheme for transport " + transportName,
          TranspAuthFailureReason.UNSUPPORTED_SCHEME);
    }

    boolean primaryMatch = !token.isBlank() && constantTimeEquals(token, settings.bearerToken());
    boolean transitionMatch = settings.NetAuthHasTransitionToken()
        && constantTimeEquals(token, settings.bearerTokenTransition());

    if (!primaryMatch && !transitionMatch) {
      throw new TranspAuthExcep(
          "Invalid bearer token for transport " + transportName,
          TranspAuthFailureReason.INVALID_TOKEN);
    }
  }

  private static boolean constantTimeEquals(String left, String right) {
    return MessageDigest.isEqual(
        left.getBytes(StandardCharsets.UTF_8),
        right.getBytes(StandardCharsets.UTF_8));
  }
}
