package mcp.server.domain.reference_data.resource;

import mcp.server.domain.reference_data.persistence.CompetencyLevelLookupJpaRepo;
import mcp.server.foundation.resource_interface.ResrcProvid;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * MCP Resource: skill-level catalog
 *
 * Exposes all available skill levels as passive reference data.
 * MCP clients can read this resource instead of issuing a tool call.
 */
public final class SkillLevelCatalogResourceProvider implements ResrcProvid {

    private final String resourceUri;
    private final String resourceName;
    private final CompetencyLevelLookupJpaRepo competencyLevelLookupRepo;

    public SkillLevelCatalogResourceProvider(
            String resourceUri,
            String resourceName,
            CompetencyLevelLookupJpaRepo competencyLevelLookupRepo) {
        this.resourceUri = Objects.requireNonNull(resourceUri, "resourceUri");
        this.resourceName = Objects.requireNonNull(resourceName, "resourceName");
        this.competencyLevelLookupRepo = Objects.requireNonNull(competencyLevelLookupRepo, "competencyLevelLookupRepo");
    }

    @Override
    public Map<String, Object> ResourceProvRead() {
        List<String> skillLevels = competencyLevelLookupRepo.findAll().stream()
                .sorted(Comparator.comparing(sl -> sl.getCompetencyLevelLookupId()))
                .map(sl -> sl.getCompetencyLevelName())
                .toList();

        return Map.of(
                "uri", resourceUri,
                "resource", resourceName,
                "trinityLayer", "Resources",
                "skillLevels", skillLevels);
    }
}
