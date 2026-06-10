package mcp.server.domain.missions.model;

public record MissionSlotRequiredSkillId(long value) {

    public MissionSlotRequiredSkillId {
        if (value < 0) {
            throw new IllegalArgumentException("MissionSlotRequiredSkillId value must not be negative: " + value);
        }
    }

    public boolean isAssigned() {
        return value > 0;
    }

    @Override
    public String toString() {
        return "MissionSlotRequiredSkillId(" + value + ")";
    }
}
