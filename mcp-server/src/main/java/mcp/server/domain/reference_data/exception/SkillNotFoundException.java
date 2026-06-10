package mcp.server.domain.reference_data.exception;

import mcp.server.domain.shared_kernel.exception.EntityNotFoundException;

/**
 * Thrown when a {@code Skill} aggregate cannot be found by its {@code SkillId}.
 */
public final class SkillNotFoundException extends EntityNotFoundException {

    public SkillNotFoundException(Object skillId) {
        super("Skill", skillId);
    }
}
