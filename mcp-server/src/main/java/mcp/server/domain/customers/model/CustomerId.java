package mcp.server.domain.customers.model;

/**
 * Typed identity for the {@link Customer} aggregate.
 *
 * <p>
 * Value {@code 0} is the transient/pre-persist state - used only briefly during
 * aggregate creation before the repository assigns a real identity on save.
 * Use {@link #isAssigned()} to distinguish assigned from transient instances.
 */
public record CustomerId(long value) {

    public CustomerId {
        if (value < 0) {
            throw new IllegalArgumentException("CustomerId value must not be negative: " + value);
        }
    }

    public boolean isAssigned() {
        return value > 0;
    }

    @Override
    public String toString() {
        return "CustomerId(" + value + ")";
    }
}
