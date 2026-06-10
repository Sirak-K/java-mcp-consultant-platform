package mcp.server.domain.missions.web;

import mcp.server.domain.missions.application.RegisteredMissionQuery;

public final class RegisteredMissionWebContract {

    private RegisteredMissionWebContract() {
    }

    public record RegisteredMissionView(
            long id,
            String status,
            String customerName,
            MissionSpecificationWebContract.SpecificationView specification,
            String createdAt,
            String updatedAt) {
    }

    public static RegisteredMissionView fromApplication(RegisteredMissionQuery.RegisteredMissionView view) {
        if (view == null) {
            return null;
        }
        return new RegisteredMissionView(
                view.id(),
                view.status(),
                view.customerName(),
                MissionSpecificationWebContract.fromApplication(view.specification()),
                view.createdAt(),
                view.updatedAt());
    }
}
