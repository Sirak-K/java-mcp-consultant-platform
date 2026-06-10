package mcp.server.domain.customers.exception;

import mcp.server.domain.shared_kernel.exception.EntityNotFoundException;

/**
 * Thrown when a {@code Customer} aggregate cannot be found by its
 * {@code CustomerId}.
 */
public final class CustomerNotFoundException extends EntityNotFoundException {

    public CustomerNotFoundException(Object customerId) {
        super("Customer", customerId);
    }
}
