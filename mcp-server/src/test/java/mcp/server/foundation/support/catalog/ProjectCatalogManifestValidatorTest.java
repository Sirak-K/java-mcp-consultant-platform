package mcp.server.foundation.support.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

class ProjectCatalogManifestValidatorTest {

  @Test
  void validatesProjectCatalogManifestAgainstCatalogInventory() {
    ProjectCatalogManifestValidator.validate(new ProjectCatalogJsonLoader(new ObjectMapper()));
  }
}
