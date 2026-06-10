package mcp.server.domain.missions.model;

import mcp.server.domain.shared_kernel.exception.DomainInvariantViolationException;
import mcp.server.domain.reference_data.model.SkillId;
import mcp.server.domain.reference_data.model.SkillLevel;

public record MissionSlotRequiredSkillInput(SkillId skillId, SkillLevel requiredSkillLevel) {

    public MissionSlotRequiredSkillInput {
        if (skillId == null) {
            throw new DomainInvariantViolationException("MissionSlotRequiredSkillInput skillId must not be null");
        }
        if (requiredSkillLevel == null) {
            throw new DomainInvariantViolationException("MissionSlotRequiredSkillInput requiredSkillLevel must not be null");
        }
    }
}
