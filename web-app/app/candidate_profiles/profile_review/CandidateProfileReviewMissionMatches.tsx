import type { CandidateProfileReviewItem } from "~/candidate_profiles/types";

export function CandidateProfileReviewMissionMatches({
  item,
}: {
  item: CandidateProfileReviewItem;
}) {
  return (
    <section className="panel-soft rounded p-4 text-sm">
      <h3 className="text-section mb-2 font-medium">Uppdragsträffar</h3>
      {item.findMissionResults.length === 0 ? (
        <p className="text-soft">Inga uppdragsträffar ännu.</p>
      ) : (
        <ul className="text-muted grid gap-2">
          {item.findMissionResults.map((result) => (
            <li key={result.missionSlotId}>
              {result.missionTitle} / Position {result.missionSlotNumber}:{" "}
              {result.roleTitle}, {result.requiredRoleExperienceYears} år
              {" - "}
              {result.score}p - {result.scoreLabel}
              {" | "}
              Roll {result.roleMatched ? "OK" : "saknas"}
              {" | "}
              Arbetsläge {result.workModeMatched ? "OK" : "saknas"}
              {" | "}
              Kompetenser {result.matchedSkillCount}/{result.requiredSkillCount}
              {result.matchedSkills.length > 0
                ? ` (${result.matchedSkills.join(", ")})`
                : ""}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
