package mcp.server.foundation.security.request_binding;

/**
 * Source of the currently active tenant context.
 */
public enum TenantBindingSource {

  AUTH_IDENTITY("auth_identity"),
  ASSUME_TENANT("assume_tenant"),
  PRE_SESSION("pre_session"),
  PLATFORM_SYSTEM("platform_system");

  private final String id;

  TenantBindingSource(String id) {
    this.id = id;
  }

  public String TenantBindingSourceGetId() {
    return id;
  }
}
