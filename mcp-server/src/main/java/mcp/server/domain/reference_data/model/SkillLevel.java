package mcp.server.domain.reference_data.model;

public enum SkillLevel {
    JUNIOR,
    INTERMEDIATE,
    SENIOR;

    public boolean satisfies(SkillLevel requiredLevel) {
        if (requiredLevel == null) {
            throw new IllegalArgumentException("requiredLevel must not be null");
        }
        return this.ordinal() >= requiredLevel.ordinal();
    }

    public static SkillLevel higherOf(SkillLevel left, SkillLevel right) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("SkillLevel values must not be null");
        }
        return left.ordinal() >= right.ordinal() ? left : right;
    }
}
