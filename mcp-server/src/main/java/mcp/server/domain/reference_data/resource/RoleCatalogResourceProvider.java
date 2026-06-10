package mcp.server.domain.reference_data.resource;

import mcp.server.domain.reference_data.persistence.RoleEntity;
import mcp.server.domain.reference_data.persistence.RoleJpaRepo;
import mcp.server.foundation.resource_interface.ResrcProvid;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * MCP Resource: consultant role catalog
 *
 * Exposes all platform consultant roles as passive reference data.
 * MCP clients can read the role catalog without issuing a tool call.
 */
public final class RoleCatalogResourceProvider implements ResrcProvid {

        private final String resourceUri;
        private final String resourceName;
        private final RoleJpaRepo roleRepo;

        public RoleCatalogResourceProvider(
                        String resourceUri,
                        String resourceName,
                        RoleJpaRepo roleRepo) {
                this.resourceUri = Objects.requireNonNull(resourceUri, "resourceUri");
                this.resourceName = Objects.requireNonNull(resourceName, "resourceName");
                this.roleRepo = Objects.requireNonNull(roleRepo, "roleRepo");
        }

        @Override
        public Map<String, Object> ResourceProvRead() {
                List<Map<String, Object>> roleList = roleRepo.findAll().stream()
                                .sorted(Comparator.comparing(RoleEntity::getId))
                                .map(r -> Map.<String, Object>of(
                                                "id", r.getId(),
                                                "title", r.getRoleTitle()))
                                .toList();

                return Map.of(
                                "uri", resourceUri,
                                "resource", resourceName,
                                "trinityLayer", "Resources",
                                "roleCount", roleList.size(),
                                "roles", roleList);
        }
}
