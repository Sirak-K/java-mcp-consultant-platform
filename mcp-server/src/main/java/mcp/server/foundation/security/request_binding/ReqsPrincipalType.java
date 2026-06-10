package mcp.server.foundation.security.request_binding;

/**
 * Canonical principal classes for the request lifecycle model.
 */
public enum ReqsPrincipalType {

  HUMAN_PRINCIPAL("human_principal"),
  SERVICE_PRINCIPAL("service_principal"),
  PLATFORM_OPS_PRINCIPAL("platform_ops_principal");

  private final String id;

  ReqsPrincipalType(String id) {
    this.id = id;
  }

  public String ReqsPrincipalTypeGetId() {
    return id;
  }
}
