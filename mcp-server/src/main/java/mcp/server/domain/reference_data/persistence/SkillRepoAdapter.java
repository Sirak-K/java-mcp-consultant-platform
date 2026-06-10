package mcp.server.domain.reference_data.persistence;

import org.springframework.stereotype.Repository;

import mcp.server.domain.reference_data.model.Skill;
import mcp.server.domain.reference_data.model.SkillId;

import java.util.List;
import java.util.Optional;

/**
 * Persistence adapter implementing {@link SkillRepo} via the split skill
 * catalog.
 */
@Repository
public class SkillRepoAdapter implements SkillRepo {

    private final PrimarySkillJpaRepo primarySkillRepo;
    private final SecondarySkillJpaRepo secondarySkillRepo;

    public SkillRepoAdapter(
            PrimarySkillJpaRepo primarySkillRepo,
            SecondarySkillJpaRepo secondarySkillRepo) {
        this.primarySkillRepo = primarySkillRepo;
        this.secondarySkillRepo = secondarySkillRepo;
    }

    @Override
    public Skill save(Skill skill) {
        SecondarySkillEntity entity = toSecondaryEntity(skill);
        SecondarySkillEntity saved = secondarySkillRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Skill> findById(SkillId id) {
        return primarySkillRepo.findById(id.value())
                .map(this::toDomain)
                .or(() -> secondarySkillRepo.findById(id.value()).map(this::toDomain));
    }

    @Override
    public Optional<Skill> findByTitle(String title) {
        return primarySkillRepo.findBySkillTitle(title)
                .map(this::toDomain)
                .or(() -> secondarySkillRepo.findBySkillTitle(title).map(this::toDomain));
    }

    @Override
    public List<Skill> findAll() {
        return java.util.stream.Stream.concat(
                primarySkillRepo.findAll().stream().map(this::toDomain),
                secondarySkillRepo.findAll().stream().map(this::toDomain))
                .toList();
    }

    @Override
    public void delete(SkillId id) {
        if (primarySkillRepo.existsById(id.value())) {
            primarySkillRepo.deleteById(id.value());
        }
        if (secondarySkillRepo.existsById(id.value())) {
            secondarySkillRepo.deleteById(id.value());
        }
    }

    @Override
    public boolean existsById(SkillId id) {
        return primarySkillRepo.existsById(id.value()) || secondarySkillRepo.existsById(id.value());
    }

    @Override
    public boolean existsPrimaryById(SkillId id) {
        return primarySkillRepo.existsById(id.value());
    }

    @Override
    public boolean existsSecondaryById(SkillId id) {
        return secondarySkillRepo.existsById(id.value());
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private SecondarySkillEntity toSecondaryEntity(Skill domain) {
        Long entityId = domain.getId().value() == 0 ? null : domain.getId().value();
        return new SecondarySkillEntity(entityId, domain.getTitle());
    }

    private Skill toDomain(PrimarySkillEntity entity) {
        return new Skill(
                new SkillId(entity.getId()),
                entity.getSkillTitle());
    }

    private Skill toDomain(SecondarySkillEntity entity) {
        return new Skill(
                new SkillId(entity.getId()),
                entity.getSkillTitle());
    }
}
