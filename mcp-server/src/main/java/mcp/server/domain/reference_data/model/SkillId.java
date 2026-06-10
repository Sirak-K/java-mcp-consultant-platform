package mcp.server.domain.reference_data.model;

/**
 * Typed identity for the {@link Skill} aggregate.
 *
 * <p>
 * Value {@code 0} is the transient/pre-persist state.
 * Use {@link #isAssigned()} to distinguish assigned from transient instances.
 */
public record SkillId(long value) {

    public SkillId {
        if (value < 0) {
            throw new IllegalArgumentException("SkillId value must not be negative: " + value);
        }
    }

    public boolean isAssigned() {
        return value > 0;
    }

    @Override
    public String toString() {
        return "SkillId(" + value + ")";
    }
}
