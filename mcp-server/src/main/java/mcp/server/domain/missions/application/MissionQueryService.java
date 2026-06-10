package mcp.server.domain.missions.application;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mcp.server.domain.missions.persistence.MissionEntity;
import mcp.server.domain.missions.persistence.MissionJpaRepository;
import mcp.server.domain.missions.persistence.MissionSlotEntity;
import mcp.server.domain.missions.persistence.MissionSlotJpaRepository;

@Service
public class MissionQueryService {

  private final MissionJpaRepository missionRepo;
  private final MissionSlotJpaRepository missionSlotRepo;
  private final MissionSpecificationAssembler specificationAssembler;
  private final RegisteredMissionViewAssembler registeredMissionViewAssembler;

  public MissionQueryService(
      MissionJpaRepository missionRepo,
      MissionSlotJpaRepository missionSlotRepo,
      MissionSpecificationAssembler specificationAssembler,
      RegisteredMissionViewAssembler registeredMissionViewAssembler) {
    this.missionRepo = missionRepo;
    this.missionSlotRepo = missionSlotRepo;
    this.specificationAssembler = specificationAssembler;
    this.registeredMissionViewAssembler = registeredMissionViewAssembler;
  }

  @Transactional(readOnly = true)
  public long countRegisteredMissions() {
    return missionRepo.count();
  }

  @Transactional(readOnly = true)
  public int countFilledSlotsByCandidateProfileId(long candidateProfileId) {
    return missionSlotRepo.countByMissionSlotFilledByProfileId(candidateProfileId);
  }

  @Transactional(readOnly = true)
  public List<RegisteredMissionQuery.RegisteredMissionView> registeredMissions() {
    return missionRepo.findAllByOrderByIdDesc().stream()
        .map(entity -> registeredMissionViewAssembler.toRegisteredView(
            entity,
            specificationAssembler.toSpecification(entity, missionSlotRepo.findByMissionId(entity.getId()))))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<RegisteredMissionQuery.MissionReadView> openMissions() {
    return missionRepo.findByMissionAvailability("OPEN").stream()
        .map(mission -> readView(mission, missionSlotRepo.findByMissionId(mission.getId())))
        .toList();
  }

  @Transactional(readOnly = true)
  public Map<Long, RegisteredMissionQuery.MissionSlotReadView> missionSlotsById(Collection<Long> missionSlotIds) {
    if (missionSlotIds == null || missionSlotIds.isEmpty()) {
      return Map.of();
    }
    Map<Long, RegisteredMissionQuery.MissionSlotReadView> slotsById = new HashMap<>();
    missionSlotRepo.findAllById(missionSlotIds.stream().filter(Objects::nonNull).distinct().toList())
        .forEach(slot -> {
          MissionEntity mission = missionRepo.findById(slot.getMissionId())
              .orElse(null);
          if (mission == null) {
            return;
          }
          readView(mission, missionSlotRepo.findByMissionId(mission.getId())).slots().stream()
              .filter(readSlot -> readSlot.missionSlotId() == safeLong(slot.getId()))
              .findFirst()
              .ifPresent(readSlot -> slotsById.put(readSlot.missionSlotId(), readSlot));
        });
    return slotsById;
  }

  @Transactional(readOnly = true)
  public Map<Long, RegisteredMissionQuery.MissionReadView> missionsById(Collection<Long> missionIds) {
    if (missionIds == null || missionIds.isEmpty()) {
      return Map.of();
    }
    Map<Long, RegisteredMissionQuery.MissionReadView> missionsById = new HashMap<>();
    missionRepo.findAllById(missionIds.stream().filter(Objects::nonNull).distinct().toList())
        .forEach(mission -> missionsById.put(
            safeLong(mission.getId()),
            readView(mission, missionSlotRepo.findByMissionId(mission.getId()))));
    return missionsById;
  }

  @Transactional(readOnly = true)
  public RegisteredMissionQuery.MissionSlotReadView requireMissionSlot(long missionSlotId) {
    return missionSlotsById(List.of(missionSlotId)).values().stream()
        .findFirst()
        .orElseThrow(() -> notFound("missionSlot not found"));
  }

  @Transactional(readOnly = true)
  public RegisteredMissionQuery.MissionReadView requireMissionForSlot(long missionSlotId) {
    RegisteredMissionQuery.MissionSlotReadView slot = requireMissionSlot(missionSlotId);
    return missionsById(List.of(slot.missionId())).get(slot.missionId());
  }

  @Transactional(readOnly = true)
  public RegisteredMissionQuery.MissionReadView requireOpenMissionForSlot(long missionSlotId) {
    RegisteredMissionQuery.MissionReadView mission = requireMissionForSlot(missionSlotId);
    if (mission == null || !"OPEN".equals(mission.missionAvailability())) {
      throw notFound("open mission not found");
    }
    return mission;
  }

  @Transactional(readOnly = true)
  public int missionSlotNumber(long missionSlotId) {
    return requireMissionSlot(missionSlotId).missionSlotNumber();
  }

  private RegisteredMissionQuery.MissionReadView readView(
      MissionEntity mission,
      List<MissionSlotEntity> slots) {

    MissionSpecification.SpecificationView specification = specificationAssembler.toSpecification(mission, slots);
    return new RegisteredMissionQuery.MissionReadView(
        safeLong(mission.getId()),
        mission.getMissionAvailability(),
        specification,
        slots.stream()
            .map(slot -> slotReadView(slot, specification))
            .toList());
  }

  private RegisteredMissionQuery.MissionSlotReadView slotReadView(
      MissionSlotEntity slot,
      MissionSpecification.SpecificationView specification) {

    MissionSpecification.SlotSpecificationView slotSpecification = specification.missionSlots().stream()
        .filter(candidate -> candidate.slotNumber() == safeInt(slot.getMissionSlotNumber()))
        .findFirst()
        .orElseGet(() -> new MissionSpecification.SlotSpecificationView(
            safeInt(slot.getMissionSlotNumber()),
            safeLong(slot.getRoleId()),
            "",
            safeInt(slot.getRequiredRoleExperienceYears()),
            List.of()));
    return new RegisteredMissionQuery.MissionSlotReadView(
        safeLong(slot.getId()),
        safeLong(slot.getMissionId()),
        safeInt(slot.getMissionSlotNumber()),
        slotSpecification);
  }

  private ResponseStatusException notFound(String reason) {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, reason);
  }

  private static long safeLong(Long value) {
    return value == null ? 0L : value;
  }

  private static int safeInt(Number value) {
    return value == null ? 0 : value.intValue();
  }
}
