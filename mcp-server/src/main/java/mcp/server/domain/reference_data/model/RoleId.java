package mcp.server.domain.reference_data.model;

/**
 * Typed identity for the {@link Role} aggregate.
 *
 * <p>
 * Value {@code 0} is the transient/pre-persist state.
 * Use {@link #isAssigned()} to distinguish assigned from transient instances.
 */
public record RoleId(long value) {

    public RoleId {
        if (value < 0) {
            throw new IllegalArgumentException("RoleId value must not be negative: " + value);
        }
    }

    public boolean isAssigned() {
        return value > 0;
    }

    @Override
    public String toString() {
        return "RoleId(" + value + ")";
    }
}
