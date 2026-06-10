package mcp.server.domain.reference_data.exception;

import mcp.server.domain.shared_kernel.exception.EntityNotFoundException;

/**
 * Thrown when a {@code Role} aggregate cannot be found by its {@code RoleId}.
 */
public final class RoleNotFoundException extends EntityNotFoundException {

    public RoleNotFoundException(Object roleId) {
        super("Role", roleId);
    }
}
