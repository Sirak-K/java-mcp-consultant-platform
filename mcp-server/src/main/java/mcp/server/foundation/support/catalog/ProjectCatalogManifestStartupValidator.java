package mcp.server.foundation.support.catalog;

import java.util.Objects;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public final class ProjectCatalogManifestStartupValidator implements ApplicationRunner {

  private final ProjectCatalogManifestService projectCatalogManifestService;

  public ProjectCatalogManifestStartupValidator(ProjectCatalogManifestService projectCatalogManifestService) {
    this.projectCatalogManifestService = Objects.requireNonNull(
        projectCatalogManifestService,
        "projectCatalogManifestService");
  }

  @Override
  public void run(ApplicationArguments args) {
    projectCatalogManifestService.validateManifest();
  }
}
