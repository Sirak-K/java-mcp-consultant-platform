package mcp.server.foundation.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public final class NetwAuthHTTPResp {

  public static final String HEADER_AUTH_REASON = "Mcp-Auth-Reason";

  private NetwAuthHTTPResp() {
  }

  public static ResponseEntity<String> NetAuthUnauthorizedText(
      NetwAuthService authService,
      TranspAuthExcep exception) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .headers(NetAuthUnauthorizedHeaders(authService, exception))
        .contentType(MediaType.TEXT_PLAIN)
        .body("Unauthorized");
  }

  public static ResponseEntity<Void> NetAuthUnauthorizedEmpty(
      NetwAuthService authService,
      TranspAuthExcep exception) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .headers(NetAuthUnauthorizedHeaders(authService, exception))
        .build();
  }

  public static HttpHeaders NetAuthUnauthorizedHeaders(
      NetwAuthService authService,
      TranspAuthExcep exception) {

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.WWW_AUTHENTICATE, authService.NetAuthWwwAuthenticateHeaderValue());

    if (exception != null && exception.TransAuthExcepGetReason() != null) {
      headers.add(HEADER_AUTH_REASON, exception.TransAuthExcepGetReason().name());
    }

    return headers;
  }
}
