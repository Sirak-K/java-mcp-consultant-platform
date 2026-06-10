package mcp.server.domain.reference_data.web;

import java.util.List;

public final class DomainStaticReferenceDataDto {

  private DomainStaticReferenceDataDto() {
  }

  public record StaticReferenceDataView(
      List<ReferenceOptionView> roles,
      List<ReferenceSkillOptionView> skills,
      List<CompetencyLevelOptionView> skillLevels) {
  }

  public record ReferenceOptionView(long id, String title) {
  }

  public record ReferenceSkillOptionView(long id, String title, String category) {
  }

  public record CompetencyLevelOptionView(short id, String name) {
  }
}
