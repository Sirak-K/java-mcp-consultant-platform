package mcp.server.domain.missions.exception;

import mcp.server.domain.shared_kernel.exception.EntityNotFoundException;

/**
 * Thrown when a {@code Mission} aggregate cannot be found by its
 * {@code MissionId}.
 */
public final class MissionNotFoundException extends EntityNotFoundException {

    public MissionNotFoundException(Object missionId) {
        super("Mission", missionId);
    }
}
