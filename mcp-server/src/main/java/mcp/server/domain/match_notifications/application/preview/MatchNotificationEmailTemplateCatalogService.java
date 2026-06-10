package mcp.server.domain.match_notifications.application.preview;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Path;
import java.util.Objects;

import mcp.server.foundation.support.catalog.ProjectCatalogJsonLoader;

import org.springframework.stereotype.Service;

import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.object;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requireCatalogId;
import static mcp.server.foundation.support.catalog.ProjectCatalogJsonSupport.requiredText;

@Service
public final class MatchNotificationEmailTemplateCatalogService {

  private static final Path MATCH_NOTIFICATION_EMAIL_TEMPLATE_CATALOG_PATH = Path.of(
      "match_notifications",
      "email_templates",
      "match_notification_email_template_catalog.json");
  private static final String CATALOG_LABEL = "Match notification email template catalog";
  private static final String EXPECTED_CATALOG_ID = "match_notification_email_template_catalog";

  private final String subject;
  private final String htmlSignalBanner;
  private final JsonNode sectionTitles;
  private final JsonNode fieldLabels;
  private final JsonNode footer;
  private final JsonNode matchSummaryTemplates;
  private final JsonNode evidenceBriefTemplates;

  public MatchNotificationEmailTemplateCatalogService(ProjectCatalogJsonLoader catalogJsonLoader) {
    Objects.requireNonNull(catalogJsonLoader, "catalogJsonLoader");
    JsonNode root = catalogJsonLoader.loadCatalogObject(MATCH_NOTIFICATION_EMAIL_TEMPLATE_CATALOG_PATH);
    requireCatalogId(root, CATALOG_LABEL, EXPECTED_CATALOG_ID);
    requiredText(root, CATALOG_LABEL, "match_notification_email_template_catalog_version");
    this.subject = requiredText(root, CATALOG_LABEL, "mail_subject");
    this.htmlSignalBanner = requiredText(root, CATALOG_LABEL, "html_signal_banner");
    this.sectionTitles = object(root, CATALOG_LABEL, "section_titles");
    this.fieldLabels = object(root, CATALOG_LABEL, "field_labels");
    this.footer = object(root, CATALOG_LABEL, "footer");
    this.matchSummaryTemplates = object(root, CATALOG_LABEL, "match_summary_templates");
    this.evidenceBriefTemplates = object(root, CATALOG_LABEL, "evidence_brief_templates");
    validateRequiredKeys();
  }

  public String subject() {
    return subject;
  }

  public String htmlSignalBanner() {
    return htmlSignalBanner;
  }

  public String newMatchDetailsTitle() {
    return sectionTitle("new_match_details");
  }

  public String matchedCandidateProfileDetailsTitle() {
    return sectionTitle("matched_candidate_profile_details");
  }

  public String matchedMissionSlotDetailsTitle() {
    return sectionTitle("matched_mission_slot_details");
  }

  public String matchGradeLabel() {
    return fieldLabel("match_grade");
  }

  public String candidateNameLabel() {
    return fieldLabel("candidate_name");
  }

  public String matchedRolesLabel() {
    return fieldLabel("matched_roles");
  }

  public String matchedSkillsLabel() {
    return fieldLabel("matched_skills");
  }

  public String customerNameLabel() {
    return fieldLabel("customer_name");
  }

  public String missionLabel() {
    return fieldLabel("mission");
  }

  public String missionSlotRoleLabel() {
    return fieldLabel("mission_slot_role");
  }

  public String missionSlotLabel() {
    return fieldLabel("mission_slot");
  }

  public String footerOpsReviewSignalLabel() {
    return footerText("ops_review_signal_label");
  }

  public String footerNoContactNotice() {
    return footerText("no_contact_notice");
  }

  public String multiSlotMatchGradeTemplate() {
    return matchSummaryTemplate("multi_slot_match_grade");
  }

  public String noMatchedSkillsText() {
    return matchSummaryTemplate("no_matched_skills");
  }

  public String skillEvidenceNoMatchedSkillsTemplate() {
    return evidenceBriefTemplate("skill_evidence_no_matched_skills");
  }

  public String skillEvidenceMatchedSkillsTemplate() {
    return evidenceBriefTemplate("skill_evidence_matched_skills");
  }

  public String fitEvidenceAlignedTemplate() {
    return evidenceBriefTemplate("fit_evidence_aligned");
  }

  public String fitEvidenceReviewRequiredTemplate() {
    return evidenceBriefTemplate("fit_evidence_review_required");
  }

  public String evidenceBriefTemplate() {
    return evidenceBriefTemplate("evidence_brief");
  }

  private void validateRequiredKeys() {
    newMatchDetailsTitle();
    matchedCandidateProfileDetailsTitle();
    matchedMissionSlotDetailsTitle();
    matchGradeLabel();
    candidateNameLabel();
    matchedRolesLabel();
    matchedSkillsLabel();
    customerNameLabel();
    missionLabel();
    missionSlotRoleLabel();
    missionSlotLabel();
    footerOpsReviewSignalLabel();
    footerNoContactNotice();
    multiSlotMatchGradeTemplate();
    noMatchedSkillsText();
    skillEvidenceNoMatchedSkillsTemplate();
    skillEvidenceMatchedSkillsTemplate();
    fitEvidenceAlignedTemplate();
    fitEvidenceReviewRequiredTemplate();
    evidenceBriefTemplate();
  }

  private String sectionTitle(String key) {
    return requiredText(sectionTitles, CATALOG_LABEL, key);
  }

  private String fieldLabel(String key) {
    return requiredText(fieldLabels, CATALOG_LABEL, key);
  }

  private String footerText(String key) {
    return requiredText(footer, CATALOG_LABEL, key);
  }

  private String matchSummaryTemplate(String key) {
    return requiredText(matchSummaryTemplates, CATALOG_LABEL, key);
  }

  private String evidenceBriefTemplate(String key) {
    return requiredText(evidenceBriefTemplates, CATALOG_LABEL, key);
  }
}
