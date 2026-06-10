package mcp.server.domain.missions.application;

import java.time.Instant;

import org.springframework.stereotype.Component;

import mcp.server.domain.missions.model.MissionProposalStatus;
import mcp.server.domain.missions.persistence.MissionProposalEntity;
import mcp.server.domain.missions.persistence.MissionProposalOutcomeEntity;
import mcp.server.domain.missions.persistence.MissionProposalOutcomeJpaRepository;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.safeText;

@Component
public class MissionProposalOutcomeRecorder {

  private final MissionProposalOutcomeJpaRepository outcomeRepo;

  public MissionProposalOutcomeRecorder(
      MissionProposalOutcomeJpaRepository outcomeRepo) {
    this.outcomeRepo = outcomeRepo;
  }

  public MissionProposalOutcomeEntity findOutcome(MissionProposalEntity entity) {
    return outcomeRepo.findByMissionProposalId(entity.getId()).orElse(null);
  }

  public MissionProposalOutcomeEntity saveOutcome(
      MissionProposalEntity entity,
      MissionProposalStatus status,
      String outcomeNote,
      boolean replaceNote) {

    Instant now = Instant.now();
    MissionProposalOutcomeEntity outcome = outcomeRepo
        .findByMissionProposalId(entity.getId())
        .orElseGet(() -> new MissionProposalOutcomeEntity(
            null,
            entity,
            status.name(),
            "",
            now,
            now));
    outcome.setMissionProposal(entity);
    outcome.setOutcomeStatus(status.name());
    if (replaceNote) {
      outcome.setOutcomeNote(safeText(outcomeNote));
    }
    outcome.setUpdatedAt(now);
    return outcomeRepo.save(outcome);
  }
}
