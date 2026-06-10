package mcp.server.foundation.spring_integration;

import jakarta.annotation.PostConstruct;
import mcp.server.domain.candidate_profiles.tool.CandidateProfileInspectionTool;
import mcp.server.domain.matching.tool.MatchDiscoveryInspectionTool;
import mcp.server.domain.matching.tool.MatchScoreBreakdownTool;
import mcp.server.domain.missions.tool.MissionProposalInputConverterTool;
import mcp.server.domain.reference_data.tool.CompanyIdentityLookupTool;
import mcp.server.domain.candidate_presentation.tool.CandidatePresentationGenerationPreparationTool;
import mcp.server.domain.candidate_presentation.tool.CandidatePresentationGenerationResultRecordingTool;
import mcp.server.domain.match_notifications.tool.MatchNotificationPreviewTool;
import mcp.server.domain.match_notifications.tool.MatchNotificationSendTool;
import mcp.server.foundation.tool_interface.ToolDefinition;
import mcp.server.foundation.tool_interface.ToolExecPolicy;
import mcp.server.foundation.tool_interface.ToolExecProperties;
import mcp.server.foundation.tool_interface.ToolInterface;
import mcp.server.foundation.tool_interface.ToolReg;

import org.springframework.context.annotation.Configuration;

import java.util.Objects;

@Configuration
public class SpringMcpDomainToolCfg {

        private final ToolReg toolReg;
        private final ToolExecProperties toolExecProperties;

        private final MissionProposalInputConverterTool missionProposalInputConverterTool;
        private final CandidateProfileInspectionTool candidateProfileInspectionTool;
        private final CompanyIdentityLookupTool companyIdentityLookupTool;
        private final MatchDiscoveryInspectionTool matchDiscoveryInspectionTool;
        private final MatchScoreBreakdownTool matchScoreBreakdownTool;
        private final CandidatePresentationGenerationPreparationTool candidatePresentationGenerationPreparationTool;
        private final CandidatePresentationGenerationResultRecordingTool candidatePresentationGenerationResultRecordingTool;
        private final MatchNotificationPreviewTool matchNotificationPreviewTool;
        private final MatchNotificationSendTool matchNotificationSendTool;

        public SpringMcpDomainToolCfg(
                        ToolReg toolReg,
                        ToolExecProperties toolExecProperties,
                        MissionProposalInputConverterTool missionProposalInputConverterTool,
                        CandidateProfileInspectionTool candidateProfileInspectionTool,
                        CompanyIdentityLookupTool companyIdentityLookupTool,
                        MatchDiscoveryInspectionTool matchDiscoveryInspectionTool,
                        MatchScoreBreakdownTool matchScoreBreakdownTool,
                        CandidatePresentationGenerationPreparationTool candidatePresentationGenerationPreparationTool,
                        CandidatePresentationGenerationResultRecordingTool candidatePresentationGenerationResultRecordingTool,
                        MatchNotificationPreviewTool matchNotificationPreviewTool,
                        MatchNotificationSendTool matchNotificationSendTool) {

                this.toolReg = Objects.requireNonNull(toolReg, "toolReg");
                this.toolExecProperties = Objects.requireNonNull(toolExecProperties, "toolExecProperties");
                this.missionProposalInputConverterTool = Objects.requireNonNull(
                                missionProposalInputConverterTool,
                                "missionProposalInputConverterTool");
                this.candidateProfileInspectionTool = Objects.requireNonNull(candidateProfileInspectionTool,
                                "candidateProfileInspectionTool");
                this.companyIdentityLookupTool = Objects.requireNonNull(companyIdentityLookupTool,
                                "companyIdentityLookupTool");
                this.matchDiscoveryInspectionTool = Objects.requireNonNull(matchDiscoveryInspectionTool,
                                "matchDiscoveryInspectionTool");
                this.matchScoreBreakdownTool = Objects.requireNonNull(matchScoreBreakdownTool,
                                "matchScoreBreakdownTool");
                this.candidatePresentationGenerationPreparationTool = Objects.requireNonNull(
                                candidatePresentationGenerationPreparationTool,
                                "candidatePresentationGenerationPreparationTool");
                this.candidatePresentationGenerationResultRecordingTool = Objects.requireNonNull(
                                candidatePresentationGenerationResultRecordingTool,
                                "candidatePresentationGenerationResultRecordingTool");
                this.matchNotificationPreviewTool = Objects.requireNonNull(
                                matchNotificationPreviewTool,
                                "matchNotificationPreviewTool");
                this.matchNotificationSendTool = Objects.requireNonNull(
                                matchNotificationSendTool,
                                "matchNotificationSendTool");
        }

        @PostConstruct
        public void registerDomainTools() {

                registerDomainToolGroup(
                                "missionProposal",
                                missionProposalInputConverterTool.previewFromTextTool());

                registerDomainToolGroup(
                                "candidateProfileInspection",
                                candidateProfileInspectionTool.inspectApplicationArchiveTool(),
                                candidateProfileInspectionTool.inspectRegisteredProfilesTool());

                registerDomainToolGroup(
                                "companyIdentity",
                                companyIdentityLookupTool.lookupTool());

                registerDomainToolGroup(
                                "matchInspection",
                                matchDiscoveryInspectionTool.inspectTool(),
                                matchScoreBreakdownTool.inspectTool());

                registerDomainToolGroup(
                                "candidatePresentation",
                                candidatePresentationGenerationPreparationTool.collectEvidenceTool(),
                                candidatePresentationGenerationPreparationTool.getGenerationContractTool(),
                                candidatePresentationGenerationResultRecordingTool.recordGeneratedContentTool(),
                                candidatePresentationGenerationResultRecordingTool.recordGenerationFailureTool());

                registerDomainToolGroup(
                                "matchNotifications",
                                matchNotificationPreviewTool.inspectPreviewsTool(),
                                matchNotificationPreviewTool.previewMatchTool(),
                                matchNotificationSendTool.sendEmailTool());
        }

        // -------------------------------------------------------------------------

        private void registerDomainToolGroup(String groupName, ToolInterface... tools) {
                Objects.requireNonNull(groupName, "groupName");
                if (groupName.isBlank()) {
                        throw new IllegalArgumentException("Domain tool group name cannot be blank");
                }
                if (tools == null || tools.length == 0) {
                        throw new IllegalStateException("Domain tool group has no tools: " + groupName);
                }
                for (ToolInterface tool : tools) {
                        registerDomainTool(groupName, tool);
                }
        }

        private void registerDomainTool(String groupName, ToolInterface tool) {
                ToolInterface requiredTool = Objects.requireNonNull(tool, groupName + " tool");
                ToolExecPolicy policy = toolExecProperties.ToolExecPropertiesResolve(requiredTool.getName());
                toolReg.ToolRegRegister(ToolDefinition.ToolDefFromToolInterface(requiredTool, policy));
        }
}
