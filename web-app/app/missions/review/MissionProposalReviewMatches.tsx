import { roleExperienceTierLabel as roleExperienceLabel } from "~/reference_data/roleExperience";
import type { MissionProposalView } from "~/missions/types";

type MissionProposalReviewMatchesProps = {
  item: MissionProposalView;
};

export function MissionProposalReviewMatches({
  item,
}: MissionProposalReviewMatchesProps) {
  return (
<section className="panel-soft rounded-2xl p-6 text-sm md:p-8">
  <h3 className="text-title mb-6 text-center text-2xl font-semibold">
    Matchningar
  </h3>
  <div
    className="grid gap-6 md:gap-0"
    style={{
      gridTemplateColumns: `repeat(${Math.max(
        item.specification.missionSlots.length,
        1,
      )}, minmax(0, 1fr))`,
    }}
  >
    {item.specification.missionSlots.map((slot, slotIndex) => {
      const slotResults = item.findCandidateResults.filter(
        (result) => result.missionSlotNumber === slot.slotNumber,
      );
      return (
        <section
          key={slot.slotNumber}
          className={`grid gap-5 px-5 text-center ${
            slotIndex === 0
              ? ""
              : "border-t border-slate-600 pt-6 md:border-l md:border-t-0 md:pt-0"
          }`}
        >
          <div className="text-label grid gap-1 leading-snug">
            <h4 className="text-lg font-semibold">
              Uppdragsposition {slot.slotNumber}
            </h4>
            <p className="text-base font-semibold">
              {roleExperienceLabel(slot.requiredRoleExperienceYears)}{" "}
              {slot.roleTitle}
            </p>
          </div>
          <div className="text-muted grid gap-2">
            {slotResults.length === 0 ? (
              <p>Inga matchningar hittades.</p>
            ) : (
              slotResults.map((result) => (
                <p
                  key={`${result.missionSlotNumber}-${result.candProfileId}`}
                >
                  {result.candName}: {result.score}p -{" "}
                  {result.scoreLabel}
                </p>
              ))
            )}
          </div>
        </section>
      );
    })}
  </div>
</section>
  );
}
