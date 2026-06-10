package mcp.server.domain.reference_data.persistence;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class SkillCatalogLookup {

    public static final String CATEGORY_PRIMARY = "PRIMARY";
    public static final String CATEGORY_SECONDARY = "SECONDARY";

    private final PrimarySkillJpaRepo primarySkillRepo;
    private final SecondarySkillJpaRepo secondarySkillRepo;

    public SkillCatalogLookup(
            PrimarySkillJpaRepo primarySkillRepo,
            SecondarySkillJpaRepo secondarySkillRepo) {
        this.primarySkillRepo = primarySkillRepo;
        this.secondarySkillRepo = secondarySkillRepo;
    }

    public List<SkillRef> findAllSkills() {
        return Stream.concat(
                primarySkillRepo.findAll().stream()
                        .map(skill -> toSkillRef(skill, CATEGORY_PRIMARY)),
                secondarySkillRepo.findAll().stream()
                        .map(skill -> toSkillRef(skill, CATEGORY_SECONDARY)))
                .toList();
    }

    public List<SkillRef> findAllPrimarySkills() {
        return primarySkillRepo.findAll().stream()
                .map(skill -> toSkillRef(skill, CATEGORY_PRIMARY))
                .toList();
    }

    public List<SkillRef> findAllSecondarySkills() {
        return secondarySkillRepo.findAll().stream()
                .map(skill -> toSkillRef(skill, CATEGORY_SECONDARY))
                .toList();
    }

    public Optional<SkillRef> findPrimarySkillById(Long skillId) {
        return skillId == null
                ? Optional.empty()
                : primarySkillRepo.findById(skillId).map(skill -> toSkillRef(skill, CATEGORY_PRIMARY));
    }

    public Optional<SkillRef> findSecondarySkillById(Long skillId) {
        return skillId == null
                ? Optional.empty()
                : secondarySkillRepo.findById(skillId).map(skill -> toSkillRef(skill, CATEGORY_SECONDARY));
    }

    public Optional<SkillRef> findAnySkillByIdPreferPrimary(Long skillId) {
        Optional<SkillRef> primarySkill = findPrimarySkillById(skillId);
        return primarySkill.isPresent() ? primarySkill : findSecondarySkillById(skillId);
    }

    public Optional<SkillRef> findAnySkillByIdPreferSecondary(Long skillId) {
        Optional<SkillRef> secondarySkill = findSecondarySkillById(skillId);
        return secondarySkill.isPresent() ? secondarySkill : findPrimarySkillById(skillId);
    }

    public SkillRef requirePrimarySkill(Long skillId) {
        return findPrimarySkillById(skillId)
                .orElseThrow(() -> new NoSuchElementException("Primary skill not found: " + skillId));
    }

    public SkillRef requireSecondarySkill(Long skillId) {
        return findSecondarySkillById(skillId)
                .orElseThrow(() -> new NoSuchElementException("Secondary skill not found: " + skillId));
    }

    public boolean isPrimarySkillId(Long skillId) {
        return findPrimarySkillById(skillId).isPresent();
    }

    private static SkillRef toSkillRef(PrimarySkillEntity skill, String category) {
        return new SkillRef(skill.getId(), skill.getSkillTitle(), category);
    }

    private static SkillRef toSkillRef(SecondarySkillEntity skill, String category) {
        return new SkillRef(skill.getId(), skill.getSkillTitle(), category);
    }

    public record SkillRef(Long id, String title, String category) {
    }
}
