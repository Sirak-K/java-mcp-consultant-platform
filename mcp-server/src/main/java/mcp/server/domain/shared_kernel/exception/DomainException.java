package mcp.server.domain.shared_kernel.exception;

/**
 * Base class for domain-level failures that are not owned by one specific
 * domain module.
 *
 * <p>
 * Domain exceptions are unchecked because callers usually cannot recover from a
 * broken domain invariant or a missing required aggregate in-place.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
