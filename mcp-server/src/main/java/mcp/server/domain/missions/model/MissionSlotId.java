package mcp.server.domain.missions.model;

public record MissionSlotId(long value) {

    public MissionSlotId {
        if (value < 0) {
            throw new IllegalArgumentException("MissionSlotId value must not be negative: " + value);
        }
    }

    public boolean isAssigned() {
        return value > 0;
    }

    @Override
    public String toString() {
        return "MissionSlotId(" + value + ")";
    }
}
