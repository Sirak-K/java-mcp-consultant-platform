package mcp.server.domain.missions.application;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Component;

import mcp.server.domain.matching.api.CandidateMatchDiscoveryResult;
import mcp.server.domain.missions.persistence.MissionProposalEntity;
import mcp.server.domain.missions.persistence.MissionProposalOutcomeEntity;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.safeText;

@Component
public class MissionProposalViewAssembler {

        private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd | HH:mm:ss");
        public MissionProposalReview.ProposalView toView(
                        MissionProposalEntity entity,
                        MissionProposalOutcomeEntity outcome,
                        MissionSpecification.SpecificationView specification,
                        List<CandidateMatchDiscoveryResult> findCandidateResults) {
                return new MissionProposalReview.ProposalView(
                                entity.getId(),
                                entity.getStatus(),
                                specification,
                                findCandidateResults,
                                outcome == null ? "" : safeText(outcome.getOutcomeNote()),
                                formatInstant(entity.getCreatedAt()),
                                formatInstant(entity.getUpdatedAt()));
        }

        private String formatInstant(Instant instant) {
                return instant == null
                                ? null
                                : TIMESTAMP_FORMAT.format(instant.atZone(ZoneId.systemDefault()));
        }
}
