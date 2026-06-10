package mcp.server.domain.missions.application;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mcp.server.domain.customers.application.CustomerRegistryService;
import mcp.server.domain.customers.model.Customer;
import mcp.server.domain.matching.api.CandidateMatchDiscoveryResult;
import mcp.server.domain.matching.api.CandidateToSlotMatchCleanup;
import mcp.server.domain.matching.api.CandidateToSlotMatchDiscovery;
import mcp.server.domain.missions.model.MissionProposalStatus;
import mcp.server.domain.missions.persistence.MissionProposalEntity;
import mcp.server.domain.missions.persistence.MissionProposalEntityMapper;
import mcp.server.domain.missions.persistence.MissionProposalJpaRepository;
import mcp.server.foundation.audit.AuditService;

@Service
public class MissionProposalReviewService {

  private static final String OPS_SOURCE = "ops";
  private static final String MISSION_PROPOSAL_RECORD_TYPE = "missionProposal";

  private final MissionProposalJpaRepository proposalRepo;
  private final MissionProposalOutcomeRecorder outcomeRecorder;
  private final MissionMaterializationService materializationService;
  private final MissionSpecificationAssembler specificationAssembler;
  private final MissionProposalEntityMapper proposalEntityMapper;
  private final MissionProposalViewAssembler viewAssembler;
  private final CustomerRegistryService customerRegistryService;
  private final CandidateToSlotMatchDiscovery matchDiscoveryService;
  private final CandidateToSlotMatchCleanup matchCleanup;
  private final AuditService auditService;

  public MissionProposalReviewService(
      MissionProposalJpaRepository proposalRepo,
      MissionProposalOutcomeRecorder outcomeRecorder,
      MissionMaterializationService materializationService,
      MissionSpecificationAssembler specificationAssembler,
      MissionProposalEntityMapper proposalEntityMapper,
      MissionProposalViewAssembler viewAssembler,
      CustomerRegistryService customerRegistryService,
      CandidateToSlotMatchDiscovery matchDiscoveryService,
      CandidateToSlotMatchCleanup matchCleanup,
      AuditService auditService) {
    this.proposalRepo = proposalRepo;
    this.outcomeRecorder = outcomeRecorder;
    this.materializationService = materializationService;
    this.specificationAssembler = specificationAssembler;
    this.proposalEntityMapper = proposalEntityMapper;
    this.viewAssembler = viewAssembler;
    this.customerRegistryService = customerRegistryService;
    this.matchDiscoveryService = matchDiscoveryService;
    this.matchCleanup = matchCleanup;
    this.auditService = auditService;
  }

  @Transactional
  public List<MissionProposalReview.ProposalView> missionProposalsForReview() {
    return proposalRepo.findAllByOrderByCreatedAtDesc().stream()
        .map(entity -> toView(entity, matchDiscoveryService.findCandidatesForMission(
            specificationAssembler.toSpecification(entity))))
        .toList();
  }

  @Transactional
  public MissionProposalReview.ProposalView editMissionProposal(
      long id,
      MissionProposalReview.ProposalEditInput request) {

    MissionProposalEntity entity = requireMissionProposal(id);
    if (isApproved(entity)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "missionProposal is already approved and cannot be edited");
    }
    MissionSpecification.SpecificationView specification = specificationAssembler.toSpecification(request);
    Customer customer = customerRegistryService.resolveOrRegister(
        specification.customerName(),
        specification.customerEmail());
    proposalEntityMapper.applyProposalEdit(entity, specification, customer.getId().value(), MissionProposalStatus.IN_REVIEW);
    entity.replaceMissionSlots(List.of());
    proposalRepo.saveAndFlush(entity);
    entity.replaceMissionSlots(proposalEntityMapper.toProposalSlotEntities(specification.missionSlots()));
    MissionProposalEntity saved = proposalRepo.save(entity);
    outcomeRecorder.saveOutcome(
        saved,
        MissionProposalStatus.IN_REVIEW,
        request.outcome(),
        true);
    recordBusinessEvent(
        "missionProposal.edited",
        MissionProposalStatus.IN_REVIEW,
        OPS_SOURCE,
        saved.getId());
    return toView(saved, matchDiscoveryService.findCandidatesForMission(specification));
  }

  @Transactional
  public MissionProposalReview.ProposalView approveMissionProposal(long id) {
    return updateStatus(id, MissionProposalStatus.APPROVED);
  }

  @Transactional
  public MissionProposalReview.ProposalView rejectMissionProposal(long id) {
    return updateStatus(id, MissionProposalStatus.REJECTED);
  }

  private MissionProposalReview.ProposalView updateStatus(
      long id,
      MissionProposalStatus status) {

    MissionProposalEntity entity = requireMissionProposal(id);
    if (isApproved(entity)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "missionProposal is already approved");
    }
    entity.setStatus(status.name());
    entity.setUpdatedAt(Instant.now());
    MissionProposalEntity saved = proposalRepo.save(entity);
    List<CandidateMatchDiscoveryResult> findCandidateResults = List.of();
    if (status == MissionProposalStatus.APPROVED) {
      MissionMaterializationService.MaterializedMission materialized = materializationService.materialize(saved);
      findCandidateResults = matchDiscoveryService.findCandidatesForMission(
          materialized.specification(),
          materialized.slotIdsByNumber());
    }
    if (status == MissionProposalStatus.REJECTED) {
      matchCleanup.removeMatchesForMissionSlots(
          materializationService.missionSlotIdsForSourceProposal(saved.getId()));
    }
    outcomeRecorder.saveOutcome(saved, status, null, false);
    recordBusinessEvent(
        "missionProposal." + status.name().toLowerCase(),
        status,
        OPS_SOURCE,
        saved.getId());
    return toView(saved, findCandidateResults);
  }

  private MissionProposalReview.ProposalView toView(
      MissionProposalEntity entity,
      List<CandidateMatchDiscoveryResult> findCandidateResults) {

    MissionSpecification.SpecificationView specification = specificationAssembler.toSpecification(entity);
    return viewAssembler.toView(
        entity,
        outcomeRecorder.findOutcome(entity),
        specification,
        findCandidateResults);
  }

  private MissionProposalEntity requireMissionProposal(long id) {
    return proposalRepo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "missionProposal not found"));
  }

  private boolean isApproved(MissionProposalEntity entity) {
    return entity != null && MissionProposalStatus.APPROVED.name().equals(entity.getStatus());
  }

  private void recordBusinessEvent(
      String eventType,
      MissionProposalStatus status,
      String source,
      Long recordId) {

    auditService.recordBusinessEvent(
        eventType,
        status.name(),
        source,
        MISSION_PROPOSAL_RECORD_TYPE,
        recordId,
        "status=" + status.name());
  }
}
