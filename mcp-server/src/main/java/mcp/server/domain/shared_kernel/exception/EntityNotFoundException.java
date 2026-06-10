package mcp.server.domain.shared_kernel.exception;

/**
 * Base class for domain-specific not-found exceptions.
 *
 * <p>
 * Throw a concrete subclass from the owning domain module instead of this base
 * type directly.
 */
public abstract class EntityNotFoundException extends DomainException {

    protected EntityNotFoundException(String aggregateName, Object id) {
        super(aggregateName + " not found: id=" + id);
    }
}
