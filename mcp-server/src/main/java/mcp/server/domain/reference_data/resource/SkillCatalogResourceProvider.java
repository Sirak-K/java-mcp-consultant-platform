package mcp.server.domain.reference_data.resource;

import mcp.server.domain.reference_data.persistence.SkillCatalogLookup;
import mcp.server.foundation.resource_interface.ResrcProvid;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * MCP Resource: technical skill catalog
 *
 * Exposes all technical skills as passive reference data.
 * MCP clients can read the skills catalog without issuing a tool call.
 */
public final class SkillCatalogResourceProvider implements ResrcProvid {

        private final String resourceUri;
        private final String resourceName;
        private final SkillCatalogLookup skillCatalogLookup;

        public SkillCatalogResourceProvider(
                        String resourceUri,
                        String resourceName,
                        SkillCatalogLookup skillCatalogLookup) {
                this.resourceUri = Objects.requireNonNull(resourceUri, "resourceUri");
                this.resourceName = Objects.requireNonNull(resourceName, "resourceName");
                this.skillCatalogLookup = Objects.requireNonNull(skillCatalogLookup, "skillCatalogLookup");
        }

        @Override
        public Map<String, Object> ResourceProvRead() {
                List<SkillCatalogLookup.SkillRef> primarySkills = skillCatalogLookup.findAllPrimarySkills();
                List<SkillCatalogLookup.SkillRef> secondarySkills = skillCatalogLookup.findAllSecondarySkills();

                List<Map<String, Object>> skillList = java.util.stream.Stream
                                .concat(primarySkills.stream(), secondarySkills.stream())
                                .sorted(Comparator.comparing(SkillCatalogLookup.SkillRef::title))
                                .map(this::skillEntry)
                                .toList();

                return Map.of(
                                "uri", resourceUri,
                                "resource", resourceName,
                                "trinityLayer", "Resources",
                                "skillCount", skillList.size(),
                                "primarySkillCount", primarySkills.size(),
                                "secondarySkillCount", secondarySkills.size(),
                                "skills", skillList);
        }

        private Map<String, Object> skillEntry(SkillCatalogLookup.SkillRef skill) {
                String idKey = SkillCatalogLookup.CATEGORY_PRIMARY.equals(skill.category())
                                ? "primarySkillId"
                                : "secondarySkillId";
                return Map.of(
                                idKey, skill.id(),
                                "title", skill.title(),
                                "category", skill.category());
        }
}
