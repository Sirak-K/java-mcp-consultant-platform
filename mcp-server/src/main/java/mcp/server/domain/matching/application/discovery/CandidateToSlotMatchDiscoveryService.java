package mcp.server.domain.matching.application.discovery;

import org.springframework.stereotype.Service;
import mcp.server.domain.missions.application.MissionSpecification;

import mcp.server.domain.candidate_profiles.api.CandidateProfileMatchingView;
import mcp.server.domain.candidate_profiles.api.CandidateProfileQuery;
import mcp.server.domain.matching.api.CandidateMatchDiscoveryResult;
import mcp.server.domain.matching.api.CandidateToSlotMatchDiscovery;
import mcp.server.domain.matching.api.MissionMatchDiscoveryResult;
import mcp.server.domain.matching.model.CandidateToMissionSlotMatchScorer;
import mcp.server.domain.missions.application.MissionQueryService;
import mcp.server.domain.missions.application.RegisteredMissionQuery;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class CandidateToSlotMatchDiscoveryService implements CandidateToSlotMatchDiscovery {

        private static final String READY_FOR_REVIEW = "READY_FOR_REVIEW";

        private final CandidateProfileQuery candidateProfileQuery;
        private final MissionQueryService missionQueryService;
        private final CandidateToSlotMatchReconciliationService reconciliationService;

        public CandidateToSlotMatchDiscoveryService(
                        CandidateProfileQuery candidateProfileQuery,
                        MissionQueryService missionQueryService,
                        CandidateToSlotMatchReconciliationService reconciliationService) {
                this.candidateProfileQuery = candidateProfileQuery;
                this.missionQueryService = missionQueryService;
                this.reconciliationService = reconciliationService;
        }

        @Override
        public List<CandidateMatchDiscoveryResult> findCandidatesForMission(
                        MissionSpecification.SpecificationView specification) {
                return findCandidatesForMission(specification, Map.of());
        }

        @Override
        public List<CandidateMatchDiscoveryResult> findCandidatesForMission(
                        MissionSpecification.SpecificationView specification,
                        Map<Integer, Long> missionSlotIdsByNumber) {
                return findCandidateMatches(specification, missionSlotIdsByNumber);
        }

        @Override
        public List<MissionMatchDiscoveryResult> findMissionsForCandidate(long candidateProfileId) {
                return findMissionMatches(candidateProfileId);
        }

        private List<CandidateMatchDiscoveryResult> findCandidateMatches(
                        MissionSpecification.SpecificationView specification,
                        Map<Integer, Long> slotIdsByNumber) {

                Instant discoveryCreatedAt = Instant.now();
                List<CandidateProfileMatchingView> cands = candidateProfileQuery.matchableCandidateProfiles();

                return specification.missionSlots().stream()
                                .flatMap(slot -> cands.stream()
                                                .map(cand -> scoreCand(
                                                                cand,
                                                                slot,
                                                                specification.workMode(),
                                                                slotIdsByNumber.get(slot.slotNumber()),
                                                                discoveryCreatedAt)))
                                .filter(result -> CandidateToMissionSlotMatchScorer.qualifiedScore(result.score()))
                                .sorted(Comparator
                                                .comparing(CandidateMatchDiscoveryResult::missionSlotNumber)
                                                .thenComparing(CandidateMatchDiscoveryResult::score,
                                                                Comparator.reverseOrder())
                                                .thenComparing(CandidateMatchDiscoveryResult::candidateName))
                                .toList();
        }

        private List<MissionMatchDiscoveryResult> findMissionMatches(long candidateProfileId) {
                CandidateProfileMatchingView cand = candidateProfileQuery.findMatchingProfile(candidateProfileId)
                                .orElse(null);
                if (cand == null) {
                        reconciliationService.removeMatchesForCandidateProfile(candidateProfileId);
                        return List.of();
                }
                Instant discoveryCreatedAt = Instant.now();
                return missionQueryService.openMissions().stream()
                                .filter(this::isMatchable)
                                .flatMap(mission -> mission.slots().stream()
                                                .map(slot -> scoreMission(cand, mission, slot, discoveryCreatedAt)))
                                .filter(result -> CandidateToMissionSlotMatchScorer.qualifiedScore(result.score()))
                                .sorted(Comparator
                                                .comparing(MissionMatchDiscoveryResult::score,
                                                                Comparator.reverseOrder())
                                                .thenComparing(MissionMatchDiscoveryResult::missionTitle)
                                                .thenComparing(MissionMatchDiscoveryResult::missionSlotNumber))
                                .toList();
        }

        private CandidateMatchDiscoveryResult scoreCand(
                        CandidateProfileMatchingView cand,
                        MissionSpecification.SlotSpecificationView slot,
                        String requiredWorkMode,
                        Long missionProposalSlotId,
                        Instant discoveryCreatedAt) {

                CandidateToMissionSlotMatchScorer.Result score = CandidateToMissionSlotMatchScorer.score(
                                new CandidateToMissionSlotMatchScorer.RequiredRole(
                                                slot.roleId(),
                                                slot.requiredRoleExperienceYears()),
                                candRoles(cand),
                                slot.requiredSkills().stream()
                                                .map(skill -> new CandidateToMissionSlotMatchScorer.RequiredSkill(
                                                                skill.skillCategory(),
                                                                skill.skillId(),
                                                                shortValueOrZero(skill.skillLevelId())))
                                                .toList(),
                                candSkills(cand),
                                requiredWorkMode,
                                cand.workMode());

                reconciliationService.reconcilePair(cand.candidateProfileId(), missionProposalSlotId, score,
                                discoveryCreatedAt);

                return new CandidateMatchDiscoveryResult(
                                slot.slotNumber(),
                                slot.roleTitle(),
                                cand.candidateProfileId(),
                                cand.displayName(),
                                score.score(),
                                score.scoreLabel(),
                                score.roleMatched(),
                                score.workModeMatched(),
                                score.matchedSkillCount(),
                                score.requiredSkillCount(),
                                score.matchedSkillTitles());
        }

        private MissionMatchDiscoveryResult scoreMission(
                        CandidateProfileMatchingView cand,
                        RegisteredMissionQuery.MissionReadView mission,
                        RegisteredMissionQuery.MissionSlotReadView slot,
                        Instant discoveryCreatedAt) {

                MissionSpecification.SlotSpecificationView slotSpecification = slot.specification();
                CandidateToMissionSlotMatchScorer.Result score = CandidateToMissionSlotMatchScorer.score(
                                new CandidateToMissionSlotMatchScorer.RequiredRole(
                                                slotSpecification.roleId(),
                                                slotSpecification.requiredRoleExperienceYears()),
                                candRoles(cand),
                                requiredSkills(slotSpecification),
                                candSkills(cand),
                                mission.specification().workMode(),
                                cand.workMode());

                reconciliationService.reconcilePair(cand.candidateProfileId(), slot.missionSlotId(), score,
                                discoveryCreatedAt);

                return new MissionMatchDiscoveryResult(
                                slot.missionSlotId(),
                                mission.specification().missionTitle(),
                                slot.missionSlotNumber(),
                                slotSpecification.roleTitle(),
                                slotSpecification.requiredRoleExperienceYears(),
                                score.score(),
                                score.scoreLabel(),
                                score.roleMatched(),
                                score.workModeMatched(),
                                score.matchedSkillCount(),
                                score.requiredSkillCount(),
                                score.matchedSkillTitles(),
                                READY_FOR_REVIEW);
        }

        private List<CandidateToMissionSlotMatchScorer.CandidateRole> candRoles(CandidateProfileMatchingView cand) {
                return cand.roles().stream()
                                .map(role -> new CandidateToMissionSlotMatchScorer.CandidateRole(
                                                role.roleId(),
                                                role.roleExperienceYears()))
                                .toList();
        }

        private List<CandidateToMissionSlotMatchScorer.CandidateSkill> candSkills(CandidateProfileMatchingView cand) {
                return cand.skills().stream()
                                .map(skill -> new CandidateToMissionSlotMatchScorer.CandidateSkill(
                                                skill.skillCategory(),
                                                skill.skillId(),
                                                skill.skillLevelId(),
                                                skill.skillTitle()))
                                .toList();
        }

        private List<CandidateToMissionSlotMatchScorer.RequiredSkill> requiredSkills(
                        MissionSpecification.SlotSpecificationView slot) {
                return slot.requiredSkills().stream()
                                .map(skill -> new CandidateToMissionSlotMatchScorer.RequiredSkill(
                                                skill.skillCategory(),
                                                skill.skillId(),
                                                shortValueOrZero(skill.skillLevelId())))
                                .toList();
        }

        private boolean isMatchable(RegisteredMissionQuery.MissionReadView mission) {
                return mission != null && "OPEN".equals(mission.missionAvailability());
        }

        private static int shortValueOrZero(Number value) {
                return value == null ? 0 : value.intValue();
        }
}
