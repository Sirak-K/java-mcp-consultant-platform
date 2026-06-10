import { optionalRoleExperienceTierLabel as roleExperienceLabelFromYears } from "~/reference_data/roleExperience";
import type { MatchViewerMissionSlotView, MatchViewerMissionView } from "~/matching/types";
import type { MissionSkillRequirementView } from "~/missions/types";
import {
  CARD_IDENTITY_TITLE_CLASS,
  CARD_INVARIANT_HEADING_CLASS,
  CARD_ROLE_FACTS_CLASS,
  CARD_ROLE_FACT_LABEL_CLASS,
  CARD_VALUE_CLASS,
  MISSION_TOP_SECTION_CLASS,
} from "./matchViewerCardStyles";
import { displayCardText, displayMatchSkillLevel } from "./matchViewerFormatting";
import { MatchViewerSkillChips } from "./MatchViewerSkillChips";

function skillText(skill: MissionSkillRequirementView): string {
  return `${displayCardText(skill.skillTitle)} (${displayMatchSkillLevel(skill.skillLevelName)})`;
}
function requiredSkillsByCategory(
  skills: MissionSkillRequirementView[],
  category: MissionSkillRequirementView["skillCategory"],
): string[] {
  return skills
    .filter((skill) => skill.skillCategory === category)
    .map(skillText);
}

export function MatchViewerMissionSlotSummary({
  mission,
  slot,
}: {
  mission: MatchViewerMissionView;
  slot: MatchViewerMissionSlotView;
}) {
  const requiredSkills = slot.requiredSkills ?? [];
  const requiredPrimarySkills = requiredSkillsByCategory(
    requiredSkills,
    "PRIMARY",
  );
  const requiredSecondarySkills = requiredSkillsByCategory(
    requiredSkills,
    "SECONDARY",
  );

  return (
    <article className="panel grid min-w-0 content-start gap-4 rounded p-4 text-left sm:p-5">
      <div className="grid min-w-0 gap-4 lg:grid-cols-[minmax(12rem,0.9fr)_minmax(10rem,0.7fr)_minmax(14rem,1fr)]">
        <section className={`${MISSION_TOP_SECTION_CLASS} text-left`}>
          <p className={CARD_INVARIANT_HEADING_CLASS}>Uppdragsgivare</p>
          <dl className="grid gap-1 text-sm">
            <div>
              <dt className={CARD_IDENTITY_TITLE_CLASS}>Namn</dt>
              <dd className={CARD_VALUE_CLASS}>
                {displayCardText(mission.customerName)}
              </dd>
            </div>
            <div>
              <dt className={CARD_IDENTITY_TITLE_CLASS}>Email</dt>
              <dd className={`${CARD_VALUE_CLASS} break-all`}>
                {displayCardText(mission.customerEmail)}
              </dd>
            </div>
          </dl>
        </section>

        <section className={`${MISSION_TOP_SECTION_CLASS} text-left`}>
          <p className={CARD_INVARIANT_HEADING_CLASS}>Uppdragsinfo</p>
          <h3 className={CARD_IDENTITY_TITLE_CLASS}>
            Uppdrag ID: {mission.missionId}
          </h3>
        </section>

        <section className={`${MISSION_TOP_SECTION_CLASS} text-left`}>
          <p className={CARD_INVARIANT_HEADING_CLASS}>Uppdragsposition</p>
          <h3 className={CARD_IDENTITY_TITLE_CLASS}>
            Position ID: {slot.missionSlotId ?? slot.missionSlotNumber}
          </h3>
          <dl className={CARD_ROLE_FACTS_CLASS}>
            <div>
              <dt className={CARD_ROLE_FACT_LABEL_CLASS}>Roll - Titel:</dt>
              <dd className={CARD_VALUE_CLASS}>
                {displayCardText(slot.roleTitle, "Unspecified role")}
              </dd>
            </div>
            <div>
              <dt className={CARD_ROLE_FACT_LABEL_CLASS}>Roll - Nivå:</dt>
              <dd className={CARD_VALUE_CLASS}>
                {roleExperienceLabelFromYears(slot.requiredRoleExperienceYears)}
              </dd>
            </div>
            <div>
              <dt className={CARD_ROLE_FACT_LABEL_CLASS}>Roll - Nivå (År):</dt>
              <dd className={CARD_VALUE_CLASS}>
                {slot.requiredRoleExperienceYears ?? "N/A"} år
              </dd>
            </div>
          </dl>
        </section>
      </div>

      <div className="h-px bg-slate-400/50" />

      <div className="grid min-w-0 gap-4 md:grid-cols-2">
        <section>
          <h4
            className={`${CARD_INVARIANT_HEADING_CLASS} mb-2 decoration-cyan-200/80 underline-offset-4`}
          >
            Kompetenskrav - Primära
          </h4>
          <MatchViewerSkillChips
            skills={requiredPrimarySkills}
            emptyText="No Primary Skill requirements."
          />
        </section>

        <section>
          <h4
            className={`${CARD_INVARIANT_HEADING_CLASS} mb-2  decoration-cyan-200/80 underline-offset-4`}
          >
            Kompetenskrav - Sekundära
          </h4>
          <MatchViewerSkillChips
            skills={requiredSecondarySkills}
            emptyText="No Secondary Skill requirements."
          />
        </section>
      </div>
    </article>
  );
}
