package mcp.server.domain.missions.exception;

import mcp.server.domain.missions.model.MissionSlotId;
import mcp.server.domain.shared_kernel.exception.EntityNotFoundException;

public class MissionSlotNotFoundException extends EntityNotFoundException {

    public MissionSlotNotFoundException(MissionSlotId id) {
        super("MissionSlot", id.value());
    }
}
