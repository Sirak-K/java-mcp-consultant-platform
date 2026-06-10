package mcp.server.domain.matching.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class CandidateToMissionSlotMatchScorerTest {

  @Test
  void scoreQualifiesCandidateWhenRoleSkillAndWorkModeMatchMissionSlot() {
    CandidateToMissionSlotMatchScorer.Result result = CandidateToMissionSlotMatchScorer.score(
        new CandidateToMissionSlotMatchScorer.RequiredRole(7L, 3),
        List.of(new CandidateToMissionSlotMatchScorer.CandidateRole(7L, 5)),
        List.of(new CandidateToMissionSlotMatchScorer.RequiredSkill("PRIMARY", 42L, 3)),
        List.of(new CandidateToMissionSlotMatchScorer.CandidateSkill("PRIMARY", 42L, 3, "Java")),
        "REMOTE",
        "remote");

    assertThat(result.score()).isEqualTo(75);
    assertThat(result.qualifiedMatch()).isTrue();
    assertThat(result.roleMatched()).isTrue();
    assertThat(result.workModeMatched()).isTrue();
    assertThat(result.matchedSkillIds()).containsExactly(42L);
    assertThat(result.matchedSkillTitles()).containsExactly("Java");
  }

  @Test
  void scoreBreakdownAttributesPersistedSnapshotToCanonicalFactors() {
    CandidateToMissionSlotMatchScorer.ScoreBreakdown breakdown =
        CandidateToMissionSlotMatchScorer.scoreBreakdownFromSnapshot(
            new CandidateToMissionSlotMatchScorer.PersistedScoreSnapshot(
                75,
                "",
                true,
                true,
                List.of(new CandidateToMissionSlotMatchScorer.RequiredSkillEvidence(
                    "Java",
                    "PRIMARY",
                    3,
                    "Senior")),
                List.of("Java"),
                List.of("Backend Developer / 3 years"),
                List.of("REMOTE")));

    assertThat(breakdown.discoveryThreshold())
        .isEqualTo(CandidateToMissionSlotMatchScorer.QUALIFIED_MATCH_SCORE);
    assertThat(breakdown.passedDiscoveryThreshold()).isTrue();
    assertThat(breakdown.factors())
        .extracting(CandidateToMissionSlotMatchScorer.ScoreFactor::factor)
        .containsExactly("Role", "Senior Skills", "Junior Skills", "Work Mode");
    assertThat(breakdown.factors())
        .filteredOn(factor -> "Senior Skills".equals(factor.factor()))
        .singleElement()
        .satisfies(factor -> {
          assertThat(factor.matched()).isTrue();
          assertThat(factor.points()).isEqualTo(5);
        });
    assertThat(breakdown.missingOrWeakFactors())
        .containsExactly("No missing score factors in the available match snapshot.");
  }
}
