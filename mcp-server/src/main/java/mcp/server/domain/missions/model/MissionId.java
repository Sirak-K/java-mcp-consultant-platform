package mcp.server.domain.missions.model;

/**
 * Typed identity for the {@link Mission} aggregate.
 *
 * <p>
 * Value {@code 0} is the transient/pre-persist state.
 * Use {@link #isAssigned()} to distinguish assigned from transient instances.
 */
public record MissionId(long value) {

    public MissionId {
        if (value < 0) {
            throw new IllegalArgumentException("MissionId value must not be negative: " + value);
        }
    }

    public boolean isAssigned() {
        return value > 0;
    }

    @Override
    public String toString() {
        return "MissionId(" + value + ")";
    }
}
