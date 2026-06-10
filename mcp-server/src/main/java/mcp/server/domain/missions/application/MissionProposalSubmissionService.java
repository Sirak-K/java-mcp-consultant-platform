package mcp.server.domain.missions.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mcp.server.domain.customers.application.CustomerRegistryService;
import mcp.server.domain.customers.model.Customer;
import mcp.server.domain.missions.application.intake.MissionProposalIntake;
import mcp.server.domain.missions.model.MissionProposalStatus;
import mcp.server.domain.missions.persistence.MissionProposalEntity;
import mcp.server.domain.missions.persistence.MissionProposalEntityMapper;
import mcp.server.domain.missions.persistence.MissionProposalJpaRepository;
import mcp.server.foundation.audit.AuditService;

@Service
public class MissionProposalSubmissionService {

  private static final String PUBLIC_SOURCE = "public";
  private static final String MISSION_PROPOSAL_RECORD_TYPE = "missionProposal";

  private final MissionProposalJpaRepository proposalRepo;
  private final MissionProposalOutcomeRecorder outcomeRecorder;
  private final MissionSpecificationAssembler specificationAssembler;
  private final MissionProposalEntityMapper proposalEntityMapper;
  private final MissionProposalViewAssembler viewAssembler;
  private final CustomerRegistryService customerRegistryService;
  private final AuditService auditService;

  public MissionProposalSubmissionService(
      MissionProposalJpaRepository proposalRepo,
      MissionProposalOutcomeRecorder outcomeRecorder,
      MissionSpecificationAssembler specificationAssembler,
      MissionProposalEntityMapper proposalEntityMapper,
      MissionProposalViewAssembler viewAssembler,
      CustomerRegistryService customerRegistryService,
      AuditService auditService) {
    this.proposalRepo = proposalRepo;
    this.outcomeRecorder = outcomeRecorder;
    this.specificationAssembler = specificationAssembler;
    this.proposalEntityMapper = proposalEntityMapper;
    this.viewAssembler = viewAssembler;
    this.customerRegistryService = customerRegistryService;
    this.auditService = auditService;
  }

  @Transactional
  public MissionProposalReview.ProposalView createMissionProposal(
      MissionProposalIntake.ProposalInput request) {

    MissionSpecification.SpecificationView specification = specificationAssembler.toSpecification(request);
    Customer customer = customerRegistryService.resolveOrRegister(
        specification.customerName(),
        specification.customerEmail());
    MissionProposalEntity entity = proposalEntityMapper.toProposalEntity(specification, customer.getId().value());
    MissionProposalEntity saved = proposalRepo.save(entity);
    recordBusinessEvent(
        "missionProposal.submitted",
        MissionProposalStatus.SUBMITTED,
        PUBLIC_SOURCE,
        saved.getId());
    return viewAssembler.toView(
        saved,
        outcomeRecorder.saveOutcome(saved, MissionProposalStatus.SUBMITTED, "", true),
        specificationAssembler.toSpecification(saved),
        java.util.List.of());
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
