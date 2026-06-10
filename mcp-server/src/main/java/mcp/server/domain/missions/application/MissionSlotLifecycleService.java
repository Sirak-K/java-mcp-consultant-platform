package mcp.server.domain.missions.application;

import mcp.server.domain.candidate_profiles.api.CandidateProfileAssignmentCommand;
import mcp.server.domain.candidate_profiles.api.CandidateProfileAssignmentEligibility;
import mcp.server.domain.candidate_profiles.api.CandidateProfileAssignmentRequirement;
import mcp.server.domain.candidate_profiles.api.CandidateProfileQuery;
import mcp.server.domain.missions.exception.MissionSlotNotFoundException;
import mcp.server.domain.reference_data.exception.RoleNotFoundException;
import mcp.server.domain.reference_data.exception.SkillNotFoundException;
import mcp.server.domain.missions.model.MissionSlot;
import mcp.server.domain.missions.model.MissionSlotFillStatus;
import mcp.server.domain.missions.model.MissionSlotId;
import mcp.server.domain.missions.model.MissionSlotRequiredSkill;
import mcp.server.domain.missions.model.MissionSlotRequiredSkillId;
import mcp.server.domain.missions.model.MissionSlotRequiredSkillInput;
import mcp.server.domain.reference_data.model.RoleId;
import mcp.server.domain.missions.persistence.MissionSlotRepository;
import mcp.server.domain.missions.persistence.MissionSlotRequiredSkillRepository;
import mcp.server.domain.reference_data.persistence.RoleRepo;
import mcp.server.domain.reference_data.persistence.SkillRepo;
import mcp.server.domain.missions.persistence.MissionRepository;
import mcp.server.domain.missions.exception.MissionNotFoundException;
import mcp.server.domain.missions.model.MissionId;
import mcp.server.domain.reference_data.model.SkillLevel;
import mcp.server.foundation.audit.AuditService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class MissionSlotLifecycleService {

    private final MissionSlotRepository missionSlotRepo;
    private final MissionRepository missionRepo;
    private final RoleRepo roleRepo;
    private final SkillRepo skillRepo;
    private final MissionSlotRequiredSkillRepository missionSlotRequiredSkillRepo;
    private final CandidateProfileQuery candidateProfileQuery;
    private final CandidateProfileAssignmentCommand candidateProfileAssignmentCommand;
    private final AuditService auditService;

    public MissionSlotLifecycleService(
            MissionSlotRepository missionSlotRepo,
            MissionRepository missionRepo,
            RoleRepo roleRepo,
            SkillRepo skillRepo,
            MissionSlotRequiredSkillRepository missionSlotRequiredSkillRepo,
            CandidateProfileQuery candidateProfileQuery,
            CandidateProfileAssignmentCommand candidateProfileAssignmentCommand,
            AuditService auditService) {
        this.missionSlotRepo = missionSlotRepo;
        this.missionRepo = missionRepo;
        this.roleRepo = roleRepo;
        this.skillRepo = skillRepo;
        this.missionSlotRequiredSkillRepo = missionSlotRequiredSkillRepo;
        this.candidateProfileQuery = candidateProfileQuery;
        this.candidateProfileAssignmentCommand = candidateProfileAssignmentCommand;
        this.auditService = auditService;
    }

    public MissionSlot create(
            MissionId missionId,
            RoleId roleId,
            List<MissionSlotRequiredSkillInput> requiredSkills,
            int requiredRoleExperienceYears) {
        if (!missionRepo.existsById(missionId)) {
            throw new MissionNotFoundException(missionId);
        }
        if (!roleRepo.existsById(roleId)) {
            throw new RoleNotFoundException(roleId);
        }
        validateRequiredRoleExperienceYears(requiredRoleExperienceYears);
        validateRequiredSkills(requiredSkills);
        int missionSlotNumber = missionSlotRepo.countByMission(missionId) + 1;
        MissionSlot missionSlot = new MissionSlot(
                new MissionSlotId(0),
                missionId,
                roleId,
                List.of(),
                requiredRoleExperienceYears,
                missionSlotNumber,
                MissionSlotFillStatus.NOT_FILLED,
                null,
                null);
        MissionSlot saved = missionSlotRepo.save(missionSlot);
        for (MissionSlotRequiredSkillInput requiredSkill : requiredSkills) {
            missionSlotRequiredSkillRepo.save(new MissionSlotRequiredSkill(
                    new MissionSlotRequiredSkillId(0),
                    saved.getId(),
                    requiredSkill.skillId(),
                    requiredSkill.requiredSkillLevel()));
        }
        return findById(saved.getId());
    }

    public MissionSlot fill(MissionSlotId missionSlotId, long candidateProfileId) {
        MissionSlot slot = loadOrThrow(missionSlotId);
        CandidateProfileAssignmentEligibility eligibility = candidateProfileQuery.evaluateAssignmentEligibility(
                assignmentRequirement(slot, candidateProfileId));
        if (!eligibility.eligible()) {
            throw new IllegalStateException(String.join(" ", eligibility.rejectionReasons()));
        }

        slot.fill(candidateProfileId, Instant.now());
        MissionSlot saved = missionSlotRepo.save(slot);

        int activeSlotCount = missionSlotRepo.countByFilledByCandidateProfileId(candidateProfileId);
        if (activeSlotCount > 5) {
            candidateProfileAssignmentCommand.markUnavailableWhenAssignmentLimitExceeded(
                    candidateProfileId,
                    activeSlotCount);
        }

        auditService.recordBusinessEvent(
                "SLOT_FILL", "SUCCESS",
                null,
                "MissionSlot", missionSlotId.value(),
                "candidateProfileId=" + candidateProfileId);
        return enrichMissionSlot(saved);
    }

    public MissionSlot update(MissionSlotId missionSlotId, RoleId roleId, Integer requiredRoleExperienceYears) {
        MissionSlot existing = loadOrThrow(missionSlotId);
        RoleId effectiveRoleId = roleId != null ? roleId : existing.getRoleId();
        int effectiveRequiredRoleExperienceYears = requiredRoleExperienceYears != null
                ? requiredRoleExperienceYears
                : existing.getRequiredRoleExperienceYears();
        if (roleId != null && !roleRepo.existsById(roleId)) {
            throw new RoleNotFoundException(roleId);
        }
        validateRequiredRoleExperienceYears(effectiveRequiredRoleExperienceYears);
        MissionSlot updated = new MissionSlot(
                existing.getId(),
                existing.getMissionId(),
                effectiveRoleId,
                existing.getRequiredSkills(),
                effectiveRequiredRoleExperienceYears,
                existing.getMissionSlotNumber(),
                existing.getFillStatus(),
                existing.getFilledByCandidateProfileId(),
                existing.getFilledAt());
        return enrichMissionSlot(missionSlotRepo.save(updated));
    }

    public MissionSlot unfill(MissionSlotId missionSlotId) {
        MissionSlot missionSlot = loadOrThrow(missionSlotId);
        Long previouslyFilledByCandidateProfileId = missionSlot.getFilledByCandidateProfileId();
        missionSlot.unfill();
        MissionSlot saved = missionSlotRepo.save(missionSlot);
        if (previouslyFilledByCandidateProfileId != null) {
            missionSlotRepo.countByFilledByCandidateProfileId(previouslyFilledByCandidateProfileId);
        }
        auditService.recordBusinessEvent(
                "SLOT_UNFILL", "SUCCESS",
                null,
                "MissionSlot", missionSlotId.value(),
                "unfilled");
        return enrichMissionSlot(saved);
    }

    public MissionSlot findById(MissionSlotId id) {
        return loadOrThrow(id);
    }

    public List<MissionSlot> listByMission(MissionId missionId) {
        return missionSlotRepo.findByMission(missionId).stream()
                .map(this::enrichMissionSlot)
                .toList();
    }

    public List<MissionSlot> findByRoleAndNotFilled(RoleId roleId) {
        return missionSlotRepo.findByRoleIdAndNotFilled(roleId).stream()
                .map(this::enrichMissionSlot)
                .toList();
    }

    public void delete(MissionSlotId id) {
        MissionSlot slot = loadOrThrow(id);
        if (missionSlotRepo.countByMission(slot.getMissionId()) <= 1) {
            throw new IllegalStateException(
                    "Cannot delete the last MissionSlot of a Mission - a Mission must always have at least one slot");
        }
        missionSlotRepo.delete(id);
    }

    private MissionSlot loadOrThrow(MissionSlotId id) {
        MissionSlot slot = missionSlotRepo.findById(id)
                .orElseThrow(() -> new MissionSlotNotFoundException(id));
        return enrichMissionSlot(slot);
    }

    private MissionSlot enrichMissionSlot(MissionSlot missionSlot) {
        List<MissionSlotRequiredSkill> requiredSkills = missionSlotRequiredSkillRepo
                .findByMissionSlotId(missionSlot.getId());
        if (requiredSkills.isEmpty() && !missionSlot.getRequiredSkills().isEmpty()) {
            requiredSkills = missionSlot.getRequiredSkills();
        }
        return new MissionSlot(
                missionSlot.getId(),
                missionSlot.getMissionId(),
                missionSlot.getRoleId(),
                requiredSkills,
                missionSlot.getRequiredRoleExperienceYears(),
                missionSlot.getMissionSlotNumber(),
                missionSlot.getFillStatus(),
                missionSlot.getFilledByCandidateProfileId(),
                missionSlot.getFilledAt());
    }

    private CandidateProfileAssignmentRequirement assignmentRequirement(MissionSlot slot, long candidateProfileId) {
        return new CandidateProfileAssignmentRequirement(
                candidateProfileId,
                slot.getRoleId().value(),
                slot.getRequiredSkills().stream()
                        .map(requiredSkill -> new CandidateProfileAssignmentRequirement.RequiredSkill(
                                requiredSkill.getSkillId().value(),
                                skillLevelId(requiredSkill.getRequiredSkillLevel())))
                        .toList());
    }

    private short skillLevelId(SkillLevel skillLevel) {
        return switch (skillLevel) {
            case JUNIOR -> 1;
            case INTERMEDIATE -> 2;
            case SENIOR -> 3;
        };
    }

    private void validateRequiredSkills(List<MissionSlotRequiredSkillInput> requiredSkills) {
        if (requiredSkills == null || requiredSkills.isEmpty()) {
            throw new IllegalArgumentException("requiredSkills must contain at least one skill");
        }
        if (requiredSkills.size() > 5) {
            throw new IllegalArgumentException("requiredSkills may contain at most 5 skills");
        }
        long distinctCount = requiredSkills.stream()
                .map(MissionSlotRequiredSkillInput::skillId)
                .distinct()
                .count();
        if (distinctCount != requiredSkills.size()) {
            throw new IllegalArgumentException("requiredSkills must not contain duplicate skillIds");
        }
        for (MissionSlotRequiredSkillInput requiredSkill : requiredSkills) {
            if (!skillRepo.existsPrimaryById(requiredSkill.skillId())) {
                throw new SkillNotFoundException(requiredSkill.skillId());
            }
        }
    }

    private void validateRequiredRoleExperienceYears(int requiredRoleExperienceYears) {
        if (requiredRoleExperienceYears <= 1) {
            throw new IllegalArgumentException("requiredRoleExperienceYears must be greater than 1");
        }
    }
}
