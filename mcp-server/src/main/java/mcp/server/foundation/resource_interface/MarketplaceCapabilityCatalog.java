package mcp.server.foundation.resource_interface;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MarketplaceCapabilityCatalog {

        private MarketplaceCapabilityCatalog() {
        }

        public record MarketplaceCapability(
                        String id,
                        String title,
                        List<String> backendEndpoints,
                        List<String> mcpTools,
                        List<String> mcpResources,
                        List<String> reactSurfaces,
                        List<String> persistenceAnchors,
                        CapabilityRiskClass riskClass) {

                public MarketplaceCapability {
                        id = requireText(id, "id");
                        title = requireText(title, "title");
                        backendEndpoints = List.copyOf(Objects.requireNonNull(backendEndpoints, "backendEndpoints"));
                        mcpTools = List.copyOf(Objects.requireNonNull(mcpTools, "mcpTools"));
                        mcpResources = List.copyOf(Objects.requireNonNull(mcpResources, "mcpResources"));
                        reactSurfaces = List.copyOf(Objects.requireNonNull(reactSurfaces, "reactSurfaces"));
                        persistenceAnchors = List
                                        .copyOf(Objects.requireNonNull(persistenceAnchors, "persistenceAnchors"));
                        riskClass = Objects.requireNonNull(riskClass, "riskClass");
                }

                public List<String> mcpSurfaces() {
                        List<String> surfaces = new ArrayList<>(mcpTools);
                        surfaces.addAll(mcpResources);
                        return List.copyOf(surfaces);
                }
        }

        public enum CapabilityRiskClass {
                READ_ONLY,
                MUTATING,
                EXTERNAL_SIDE_EFFECT
        }

        private static String requireText(String value, String fieldName) {
                if (value == null || value.isBlank()) {
                        throw new IllegalArgumentException(fieldName + " must not be blank");
                }
                return value;
        }
}
