package mcp.server.domain.missions.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import mcp.server.domain.candidate_profiles.api.CandidateProfileAssignmentCommand;
import mcp.server.domain.candidate_profiles.api.CandidateProfileAssignmentEligibility;
import mcp.server.domain.candidate_profiles.api.CandidateProfileAssignmentRequirement;
import mcp.server.domain.candidate_profiles.api.CandidateProfileQuery;
import mcp.server.domain.missions.model.MissionId;
import mcp.server.domain.missions.model.MissionSlot;
import mcp.server.domain.missions.model.MissionSlotFillStatus;
import mcp.server.domain.missions.model.MissionSlotId;
import mcp.server.domain.missions.model.MissionSlotRequiredSkill;
import mcp.server.domain.missions.model.MissionSlotRequiredSkillId;
import mcp.server.domain.missions.persistence.MissionRepository;
import mcp.server.domain.missions.persistence.MissionSlotRepository;
import mcp.server.domain.missions.persistence.MissionSlotRequiredSkillRepository;
import mcp.server.domain.reference_data.model.RoleId;
import mcp.server.domain.reference_data.model.SkillId;
import mcp.server.domain.reference_data.model.SkillLevel;
import mcp.server.domain.reference_data.persistence.RoleRepo;
import mcp.server.domain.reference_data.persistence.SkillRepo;
import mcp.server.foundation.audit.AuditService;

class MissionSlotLifecycleServiceTest {

    @Test
    void fillUsesCandidateProfileAssignmentBoundaryAndMarksUnavailableAfterAssignmentLimit() {
        Fixture fixture = new Fixture();
        MissionSlotId missionSlotId = new MissionSlotId(5L);
        long candidateProfileId = 10L;
        MissionSlotRequiredSkill requiredSkill = requiredSkill(missionSlotId);
        when(fixture.missionSlotRepo.findById(missionSlotId)).thenReturn(Optional.of(slot(missionSlotId)));
        when(fixture.requiredSkillRepo.findByMissionSlotId(missionSlotId)).thenReturn(List.of(requiredSkill));
        when(fixture.candidateProfileQuery.evaluateAssignmentEligibility(argThat(requirement ->
                requirement.candidateProfileId() == candidateProfileId
                        && requirement.requiredRoleId() == 7L
                        && requirement.requiredSkills().size() == 1
                        && requirement.requiredSkills().getFirst().skillId() == 42L
                        && requirement.requiredSkills().getFirst().minimumSkillLevelId() == 3)))
                .thenReturn(new CandidateProfileAssignmentEligibility(
                        candidateProfileId,
                        true,
                        true,
                        true,
                        true,
                        true,
                        List.of()));
        when(fixture.missionSlotRepo.save(any(MissionSlot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(fixture.missionSlotRepo.countByFilledByCandidateProfileId(candidateProfileId)).thenReturn(6);

        MissionSlot filled = fixture.service.fill(missionSlotId, candidateProfileId);

        assertThat(filled.getFillStatus()).isEqualTo(MissionSlotFillStatus.FILLED);
        assertThat(filled.getFilledByCandidateProfileId()).isEqualTo(candidateProfileId);
        verify(fixture.missionSlotRepo).save(argThat(saved ->
                saved.getFilledByCandidateProfileId().equals(candidateProfileId)
                        && saved.getFillStatus() == MissionSlotFillStatus.FILLED));
        verify(fixture.candidateProfileAssignmentCommand)
                .markUnavailableWhenAssignmentLimitExceeded(candidateProfileId, 6);
        verify(fixture.auditService).recordBusinessEvent(
                "SLOT_FILL",
                "SUCCESS",
                null,
                "MissionSlot",
                missionSlotId.value(),
                "candidateProfileId=" + candidateProfileId);
    }

    @Test
    void fillRejectsWhenCandidateProfileAssignmentBoundaryRejects() {
        Fixture fixture = new Fixture();
        MissionSlotId missionSlotId = new MissionSlotId(5L);
        long candidateProfileId = 10L;
        when(fixture.missionSlotRepo.findById(missionSlotId)).thenReturn(Optional.of(slot(missionSlotId)));
        when(fixture.requiredSkillRepo.findByMissionSlotId(missionSlotId)).thenReturn(List.of(requiredSkill(missionSlotId)));
        when(fixture.candidateProfileQuery.evaluateAssignmentEligibility(any(CandidateProfileAssignmentRequirement.class)))
                .thenReturn(new CandidateProfileAssignmentEligibility(
                        candidateProfileId,
                        true,
                        false,
                        true,
                        true,
                        false,
                        List.of("Candidate profile is unavailable for assignment.")));

        assertThatThrownBy(() -> fixture.service.fill(missionSlotId, candidateProfileId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Candidate profile is unavailable for assignment.");

        verify(fixture.missionSlotRepo, never()).save(any(MissionSlot.class));
        verify(fixture.candidateProfileAssignmentCommand, never())
                .markUnavailableWhenAssignmentLimitExceeded(anyLong(), anyInt());
    }

    private static MissionSlot slot(MissionSlotId id) {
        return new MissionSlot(
                id,
                new MissionId(3L),
                new RoleId(7L),
                List.of(),
                5,
                1,
                MissionSlotFillStatus.NOT_FILLED,
                null,
                null);
    }

    private static MissionSlotRequiredSkill requiredSkill(MissionSlotId missionSlotId) {
        return new MissionSlotRequiredSkill(
                new MissionSlotRequiredSkillId(11L),
                missionSlotId,
                new SkillId(42L),
                SkillLevel.SENIOR);
    }

    private static final class Fixture {
        private final MissionSlotRepository missionSlotRepo = mock(MissionSlotRepository.class);
        private final MissionRepository missionRepo = mock(MissionRepository.class);
        private final RoleRepo roleRepo = mock(RoleRepo.class);
        private final SkillRepo skillRepo = mock(SkillRepo.class);
        private final MissionSlotRequiredSkillRepository requiredSkillRepo =
                mock(MissionSlotRequiredSkillRepository.class);
        private final CandidateProfileQuery candidateProfileQuery = mock(CandidateProfileQuery.class);
        private final CandidateProfileAssignmentCommand candidateProfileAssignmentCommand =
                mock(CandidateProfileAssignmentCommand.class);
        private final AuditService auditService = mock(AuditService.class);
        private final MissionSlotLifecycleService service = new MissionSlotLifecycleService(
                missionSlotRepo,
                missionRepo,
                roleRepo,
                skillRepo,
                requiredSkillRepo,
                candidateProfileQuery,
                candidateProfileAssignmentCommand,
                auditService);
    }
}
