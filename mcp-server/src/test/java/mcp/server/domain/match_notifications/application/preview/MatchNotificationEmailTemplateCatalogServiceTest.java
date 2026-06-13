package mcp.server.domain.match_notifications.application.preview;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import mcp.server.foundation.support.catalog.ProjectCatalogJsonLoader;

class MatchNotificationEmailTemplateCatalogServiceTest {

  private final MatchNotificationEmailTemplateCatalogService catalogService =
      new MatchNotificationEmailTemplateCatalogService(new ProjectCatalogJsonLoader(new ObjectMapper()));

  @Test
  void loadsEmployerFacingMatchNotificationEmailTemplateContract() {
    assertThat(catalogService.subject()).isEqualTo("New candidate match available");
    assertThat(catalogService.htmlSignalBanner()).isEqualTo("Consultant To Mission Match Notification");
    assertThat(catalogService.matchedCandidateProfileDetailsTitle())
        .isEqualTo("Matched Candidate Profile Details");
    assertThat(catalogService.candidateNameLabel()).isEqualTo("Candidate Name");
    assertThat(catalogService.customerNameLabel()).isEqualTo("Customer Name");
    assertThat(catalogService.footerNoContactNotice())
        .isEqualTo("No customer or candidate has been contacted.");
    assertThat(catalogService.evidenceBriefTemplate()).contains("{candidateName}");

    String joinedCatalogText = String.join(" ",
        catalogService.subject(),
        catalogService.htmlSignalBanner(),
        catalogService.matchedCandidateProfileDetailsTitle(),
        catalogService.candidateNameLabel(),
        catalogService.customerNameLabel(),
        catalogService.footerNoContactNotice(),
        catalogService.evidenceBriefTemplate());
    assertThat(joinedCatalogText)
        .doesNotContain("Veltare", "AMPM", "Cand Name", "Cust Company", "No cust or cand", "{candName}");
  }
}
