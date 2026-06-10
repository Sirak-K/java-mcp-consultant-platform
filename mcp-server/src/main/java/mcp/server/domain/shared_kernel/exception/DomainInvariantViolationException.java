package mcp.server.domain.shared_kernel.exception;

/**
 * Thrown when an operation would violate a domain invariant before persistence
 * or external side effects occur.
 */
public final class DomainInvariantViolationException extends DomainException {

    public DomainInvariantViolationException(String message) {
        super(message);
    }
}
