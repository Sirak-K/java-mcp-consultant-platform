package mcp.server.domain.reference_data.persistence;

import java.util.List;
import java.util.Optional;

import mcp.server.domain.reference_data.model.Skill;
import mcp.server.domain.reference_data.model.SkillId;

/**
 * Stable domain contract for Skill catalog aggregate persistence.
 *
 * <p>
 * This interface is the only permitted boundary through which Skill catalog
 * entries may cross the persistence layer. No persistence-layer type may
 * leak through this contract.
 *
 * <p>
 * Skill is a shared reference aggregate — it is referenced by both CandSkill
 * and MissionSkillReqr. It is never owned by either. Writes to this
 * catalog must go through this contract exclusively.
 *
 */
public interface SkillRepo {

    Skill save(Skill skill);

    Optional<Skill> findById(SkillId id);

    Optional<Skill> findByTitle(String title);

    List<Skill> findAll();

    void delete(SkillId id);

    boolean existsById(SkillId id);

    boolean existsPrimaryById(SkillId id);

    boolean existsSecondaryById(SkillId id);
}
