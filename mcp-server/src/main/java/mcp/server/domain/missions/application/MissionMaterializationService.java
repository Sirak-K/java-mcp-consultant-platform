package mcp.server.domain.missions.application;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import mcp.server.domain.missions.persistence.MissionEntity;
import mcp.server.domain.missions.persistence.MissionJpaRepository;
import mcp.server.domain.missions.persistence.MissionMaterializationEntityMapper;
import mcp.server.domain.missions.persistence.MissionProposalEntity;
import mcp.server.domain.missions.persistence.MissionSlotEntity;
import mcp.server.domain.missions.persistence.MissionSlotJpaRepository;

@Service
public class MissionMaterializationService {

  private final MissionJpaRepository missionRepo;
  private final MissionSlotJpaRepository missionSlotRepo;
  private final MissionSpecificationAssembler specificationAssembler;
  private final MissionMaterializationEntityMapper materializationEntityMapper;

  public MissionMaterializationService(
      MissionJpaRepository missionRepo,
      MissionSlotJpaRepository missionSlotRepo,
      MissionSpecificationAssembler specificationAssembler,
      MissionMaterializationEntityMapper materializationEntityMapper) {
    this.missionRepo = missionRepo;
    this.missionSlotRepo = missionSlotRepo;
    this.specificationAssembler = specificationAssembler;
    this.materializationEntityMapper = materializationEntityMapper;
  }

  public MaterializedMission materialize(MissionProposalEntity proposal) {
    MissionSpecification.SpecificationView specification = specificationAssembler.toSpecification(proposal);
    MissionEntity mission = missionRepo
        .findBySourceMissionProposalId(proposal.getId())
        .orElseGet(() -> materializationEntityMapper.toMissionEntity(proposal, specification));
    materializationEntityMapper.applyMission(mission, specification, proposal.getId(), proposal.getCustomerId());
    MissionEntity savedMission = missionRepo.saveAndFlush(mission);
    List<MissionSlotEntity> existingSlots = missionSlotRepo.findByMissionId(savedMission.getId());
    if (!existingSlots.isEmpty()) {
      missionSlotRepo.deleteAll(existingSlots);
      missionSlotRepo.flush();
    }
    missionSlotRepo.saveAll(materializationEntityMapper.toRegisteredSlotEntities(
        savedMission.getId(),
        specification.missionSlots()));
    missionSlotRepo.flush();
    List<MissionSlotEntity> missionSlots = missionSlotRepo.findByMissionId(savedMission.getId());
    return new MaterializedMission(
        savedMission.getId(),
        missionSlots,
        specification,
        missionSlots.stream()
            .collect(Collectors.toMap(
                MissionSlotEntity::getMissionSlotNumber,
                MissionSlotEntity::getId,
                (left, right) -> left)));
  }

  public List<Long> missionSlotIdsForSourceProposal(long missionProposalId) {
    return missionRepo.findBySourceMissionProposalId(missionProposalId)
        .map(mission -> missionSlotRepo.findByMissionId(mission.getId()).stream()
            .map(MissionSlotEntity::getId)
            .toList())
        .orElse(List.of());
  }

  public record MaterializedMission(
      long missionId,
      List<MissionSlotEntity> missionSlots,
      MissionSpecification.SpecificationView specification,
      Map<Integer, Long> slotIdsByNumber) {
  }
}
