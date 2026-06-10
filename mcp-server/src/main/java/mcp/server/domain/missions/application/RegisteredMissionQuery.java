package mcp.server.domain.missions.application;

import java.util.List;

public final class RegisteredMissionQuery {

    private RegisteredMissionQuery() {
    }

    public record RegisteredMissionView(
            long id,
            String status,
            String customerName,
            MissionSpecification.SpecificationView specification,
            String createdAt,
            String updatedAt) {
    }

    public record MissionSlotReadView(
            long missionSlotId,
            long missionId,
            int missionSlotNumber,
            MissionSpecification.SlotSpecificationView specification) {
    }

    public record MissionReadView(
            long missionId,
            String missionAvailability,
            MissionSpecification.SpecificationView specification,
            List<MissionSlotReadView> slots) {
    }
}
