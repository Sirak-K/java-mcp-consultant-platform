package mcp.server.foundation.support.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class ProjectCatalogManifestServiceTest {

  @Test
  void validatesProjectCatalogManifestAgainstCatalogRoot() {
    ProjectCatalogManifestService service = new ProjectCatalogManifestService(
        new ProjectCatalogJsonLoader(new ObjectMapper()));

    ProjectCatalogManifestService.ValidationReport report = service.validateManifest();

    assertThat(report.catalogCount()).isEqualTo(15);
  }
}
