package mcp.server.domain.matching.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CandidateToSlotMatchJpaRepository extends JpaRepository<CandidateToSlotMatchEntity, Long> {

        List<CandidateToSlotMatchEntity> findAllByOrderByCreatedAtDesc();

        List<CandidateToSlotMatchEntity> findAllByCandidateProfileIdAndCreatedAtOrderByMissionSlotIdAsc(
                        Long candidateProfileId,
                        Instant createdAt);

        Optional<CandidateToSlotMatchEntity> findFirstByOrderByCreatedAtDesc();

        Optional<CandidateToSlotMatchEntity> findByCandidateProfileIdAndMissionSlotId(
                        Long candidateProfileId,
                        Long missionSlotId);

        @Modifying
        @Query(value = "INSERT INTO marketplace.candidate_to_slot_match ("
                        + "cand_profile_id, mission_slot_id, match_score, match_label, "
                        + "role_matched, work_mode_matched, matched_skill_count, matched_skill_ids, "
                        + "matched_skill_titles, created_at"
                        + ") VALUES ("
                        + ":candidateProfileId, :missionSlotId, :matchScore, :matchLabel, "
                        + ":roleMatched, :workModeMatched, :matchedSkillCount, :matchedSkillIds, "
                        + ":matchedSkillTitles, :createdAt"
                        + ") ON CONFLICT (cand_profile_id, mission_slot_id) DO UPDATE SET "
                        + "match_score = EXCLUDED.match_score, "
                        + "match_label = EXCLUDED.match_label, "
                        + "role_matched = EXCLUDED.role_matched, "
                        + "work_mode_matched = EXCLUDED.work_mode_matched, "
                        + "matched_skill_count = EXCLUDED.matched_skill_count, "
                        + "matched_skill_ids = EXCLUDED.matched_skill_ids, "
                        + "matched_skill_titles = EXCLUDED.matched_skill_titles", nativeQuery = true)
        int upsertQualifiedMatch(
                        @Param("candidateProfileId") Long candidateProfileId,
                        @Param("missionSlotId") Long missionSlotId,
                        @Param("matchScore") Integer matchScore,
                        @Param("matchLabel") String matchLabel,
                        @Param("roleMatched") Boolean roleMatched,
                        @Param("workModeMatched") Boolean workModeMatched,
                        @Param("matchedSkillCount") Integer matchedSkillCount,
                        @Param("matchedSkillIds") String matchedSkillIds,
                        @Param("matchedSkillTitles") String matchedSkillTitles,
                        @Param("createdAt") Instant createdAt);

        @Modifying
        int deleteByCandidateProfileIdAndMissionSlotId(
                        Long candidateProfileId,
                        Long missionSlotId);

        @Modifying
        int deleteByCandidateProfileId(Long candidateProfileId);

        @Modifying
        int deleteByMissionSlotId(Long missionSlotId);
}
