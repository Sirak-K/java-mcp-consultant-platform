package mcp.server.foundation.security.enforcement;

import mcp.server.foundation.security.request_binding.ReqsAuthBinding;

import java.util.Optional;

/**
 * ThreadLocal request-bound platform context.
 */
public final class TenantCtxHolder {

    public record Binding(boolean platformSystem) {
        public boolean isPlatformSystem() { return platformSystem; }
    }

    private static final ThreadLocal<Binding> HOLDER = new ThreadLocal<>();

    private TenantCtxHolder() {}

    public static void setPlatformSystem() {
        HOLDER.set(new Binding(true));
    }

    public static void setFromReqsAuthBinding(ReqsAuthBinding requestAuthBinding) {
        if (requestAuthBinding.platformSystem()) {
            setPlatformSystem();
            return;
        }
    }

    public static Optional<Binding> current() {
        return Optional.ofNullable(HOLDER.get());
    }

    public static void clear() {
        HOLDER.remove();
    }
}
