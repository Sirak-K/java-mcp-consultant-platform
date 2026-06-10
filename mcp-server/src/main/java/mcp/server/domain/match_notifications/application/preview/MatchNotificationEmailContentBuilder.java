package mcp.server.domain.match_notifications.application.preview;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public final class MatchNotificationEmailContentBuilder {

  private final MatchNotificationEmailTemplateCatalogService templateCatalog;

  public MatchNotificationEmailContentBuilder(MatchNotificationEmailTemplateCatalogService templateCatalog) {
    this.templateCatalog = Objects.requireNonNull(templateCatalog, "templateCatalog");
  }

  public record MailContent(String subject, String textBody, String htmlBody) {
  }

  public record MailContentInput(
      String candidateName,
      String matchedRoles,
      List<MissionSlotLine> missionSlotLines,
      String customerName,
      String missionTitle,
      String matchGrade,
      String matchedSkills,
      String evidenceBrief) {
  }

  public record MissionSlotLine(int slotNumber, String roleTitle, String matchGrade, String matchedSkills) {
  }

  public MailContent build(MailContentInput input) {
    return new MailContent(
        templateCatalog.subject(),
        buildTextBody(input),
        buildHtmlBody(input));
  }

  private String buildTextBody(MailContentInput input) {
    return String.join("\n",
        input.evidenceBrief(),
        "",
        templateCatalog.newMatchDetailsTitle(),
        templateCatalog.matchGradeLabel() + ": " + input.matchGrade(),
        "",
        templateCatalog.matchedCandidateProfileDetailsTitle(),
        templateCatalog.candidateNameLabel() + ": " + input.candidateName(),
        templateCatalog.matchedRolesLabel() + ": " + input.matchedRoles(),
        templateCatalog.matchedSkillsLabel() + ": " + input.matchedSkills(),
        "",
        templateCatalog.matchedMissionSlotDetailsTitle(),
        templateCatalog.customerNameLabel() + ": " + input.customerName(),
        templateCatalog.missionLabel() + ": " + input.missionTitle(),
        missionSlotDetailsText(input.missionSlotLines()),
        "",
        templateCatalog.footerOpsReviewSignalLabel() + ". " + templateCatalog.footerNoContactNotice());
  }

  private String buildHtmlBody(MailContentInput input) {
    String surfaceStyle = "margin:0;background:#020617;color:#dceaf8;font-family:Bahnschrift,'Segoe UI',Arial,sans-serif;";
    String panelStyle = "max-width:760px;margin:0 auto;background:#07111f;border:1px solid #155e75;border-radius:6px;overflow:hidden;";
    String bodyStyle = "padding:28px;background:linear-gradient(180deg,#07111f 0%,#020617 100%);";
    String titleStyle = "margin:26px 0 18px;color:#f7fbff;font-size:30px;line-height:1.15;font-weight:800;";
    String sectionStyle = "margin:28px 0 12px;color:#a5f3fc;font-size:18px;line-height:1.25;font-weight:800;";
    String lineStyle = "margin:0 0 12px;color:#dceaf8;font-size:16px;line-height:1.45;";
    String labelStyle = "color:#ffffff;font-weight:800;";
    return "<!doctype html><html><body style=\"" + surfaceStyle + "\">"
        + "<div style=\"" + panelStyle + "\">"
        + "<div style=\"padding:16px 28px;background:#020617;border-bottom:1px solid #155e75;color:#67e8f9;font-size:12px;font-weight:800;text-transform:uppercase;\">"
        + escapeHtml(templateCatalog.htmlSignalBanner()) + "</div>"
        + "<div style=\"" + bodyStyle + "\">"
        + "<p style=\"margin:0 0 18px;color:#e2f6ff;font-size:17px;line-height:1.55;font-weight:700;\">"
        + escapeHtml(input.evidenceBrief()) + "</p>"
        + "<h2 style=\"" + titleStyle + "\">" + escapeHtml(templateCatalog.newMatchDetailsTitle()) + "</h2>"
        + "<p style=\"" + lineStyle + "\"><span style=\"" + labelStyle + "\">"
        + escapeHtml(templateCatalog.matchGradeLabel()) + ":</span> "
        + escapeHtml(input.matchGrade()) + "</p>"
        + "<h3 style=\"" + sectionStyle + "\">" + escapeHtml(templateCatalog.matchedCandidateProfileDetailsTitle())
        + "</h3>"
        + "<p style=\"" + lineStyle + "\"><span style=\"" + labelStyle + "\">"
        + escapeHtml(templateCatalog.candidateNameLabel()) + ":</span> "
        + escapeHtml(input.candidateName()) + "</p>"
        + "<p style=\"" + lineStyle + "\"><span style=\"" + labelStyle + "\">"
        + escapeHtml(templateCatalog.matchedRolesLabel()) + ":</span> "
        + escapeHtml(input.matchedRoles()) + "</p>"
        + "<p style=\"" + lineStyle + "\"><span style=\"" + labelStyle + "\">"
        + escapeHtml(templateCatalog.matchedSkillsLabel()) + ":</span> "
        + escapeHtml(input.matchedSkills()) + "</p>"
        + "<h3 style=\"" + sectionStyle + "\">" + escapeHtml(templateCatalog.matchedMissionSlotDetailsTitle())
        + "</h3>"
        + "<p style=\"" + lineStyle + "\"><span style=\"" + labelStyle + "\">"
        + escapeHtml(templateCatalog.customerNameLabel()) + ":</span> "
        + escapeHtml(input.customerName()) + "</p>"
        + "<p style=\"" + lineStyle + "\"><span style=\"" + labelStyle + "\">"
        + escapeHtml(templateCatalog.missionLabel()) + ":</span> "
        + escapeHtml(input.missionTitle()) + "</p>"
        + missionSlotDetailsHtml(input.missionSlotLines())
        + "<p style=\"margin:28px 0 0;color:#f7fbff;font-size:15px;line-height:1.45;\"><span style=\"" + labelStyle
        + "\">" + escapeHtml(templateCatalog.footerOpsReviewSignalLabel()) + ".</span> "
        + escapeHtml(templateCatalog.footerNoContactNotice()) + "</p>"
        + "</div></div></body></html>";
  }

  private String missionSlotDetailsText(List<MissionSlotLine> lines) {
    if (lines.size() == 1) {
      return templateCatalog.missionSlotRoleLabel() + ": " + lines.get(0).roleTitle();
    }
    return lines.stream()
        .map(line -> templateCatalog.missionSlotLabel() + " " + line.slotNumber()
            + ": " + line.roleTitle()
            + " | " + line.matchGrade()
            + " | " + templateCatalog.matchedSkillsLabel() + ": " + line.matchedSkills())
        .collect(Collectors.joining("\n"));
  }

  private String missionSlotDetailsHtml(List<MissionSlotLine> lines) {
    String lineStyle = "margin:0 0 12px;color:#dceaf8;font-size:16px;line-height:1.45;";
    String labelStyle = "color:#ffffff;font-weight:800;";
    if (lines.size() == 1) {
      return "<p style=\"" + lineStyle + "\"><span style=\"" + labelStyle + "\">"
          + escapeHtml(templateCatalog.missionSlotRoleLabel()) + ":</span> "
          + escapeHtml(lines.get(0).roleTitle()) + "</p>";
    }
    return lines.stream()
        .map(line -> "<p style=\"" + lineStyle + "\"><span style=\"" + labelStyle + "\">"
            + escapeHtml(templateCatalog.missionSlotLabel())
            + " "
            + line.slotNumber()
            + ":</span> "
            + escapeHtml(line.roleTitle())
            + " | "
            + escapeHtml(line.matchGrade())
            + " | <span style=\"" + labelStyle + "\">" + escapeHtml(templateCatalog.matchedSkillsLabel()) + ":</span> "
            + escapeHtml(line.matchedSkills())
            + "</p>")
        .collect(Collectors.joining());
  }

  private static String escapeHtml(String value) {
    return textOrFallback(value, "")
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

  private static String textOrFallback(String value, String fallback) {
    return value == null || value.trim().isBlank() ? fallback : value.trim();
  }
}
