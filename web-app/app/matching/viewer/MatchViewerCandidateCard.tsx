import {
  CANDIDATE_APPLICATION_WORK_MODE_LABELS,
  formatWorkMode,
} from "~/reference_data/workMode";
import type {
  RegisteredCandidateProfileCardSkillView,
  RegisteredCandidateProfileCardView,
} from "~/candidate_profiles/types";
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

function cardSkillText(skill: RegisteredCandidateProfileCardSkillView): string {
  return `${displayCardText(skill.title)} (${displayMatchSkillLevel(skill.skillLevel)})`;
}

export function MatchViewerCandidateCard({
  candidate,
}: {
  candidate: RegisteredCandidateProfileCardView;
}) {
  const primarySkills = (candidate.primarySkills ?? []).map(cardSkillText);
  const secondarySkills = (candidate.secondarySkills ?? []).map(cardSkillText);

  return (
    <article className="panel grid min-w-0 content-start gap-4 rounded p-4 text-left sm:p-5">
      <div className="grid min-w-0 gap-4 lg:grid-cols-[minmax(14rem,1fr)_minmax(14rem,0.9fr)_minmax(12rem,0.75fr)]">
        <header className={`${MISSION_TOP_SECTION_CLASS} min-w-0`}>
          <p className={CARD_INVARIANT_HEADING_CLASS}>Kandidat</p>
          <h3 className={CARD_IDENTITY_TITLE_CLASS}>
            {displayCardText(candidate.candidateName, "Unnamed candidate")}
          </h3>
          <dl className={CARD_ROLE_FACTS_CLASS}>
            <div>
              <dt className={CARD_ROLE_FACT_LABEL_CLASS}>Roll - Titel</dt>
              <dd className={CARD_VALUE_CLASS}>
                {displayCardText(candidate.roleTitle, "Role N/A")}
              </dd>
            </div>
            <div>
              <dt className={CARD_ROLE_FACT_LABEL_CLASS}>Roll - Nivå</dt>
              <dd className={CARD_VALUE_CLASS}>
                {displayCardText(candidate.roleExperienceLevel)}
              </dd>
            </div>
            <div>
              <dt className={CARD_ROLE_FACT_LABEL_CLASS}>Roll - Nivå (År)</dt>
              <dd className={CARD_VALUE_CLASS}>
                {candidate.roleExperienceYears ?? "N/A"} år
              </dd>
            </div>
          </dl>
        </header>

        <section className={`${MISSION_TOP_SECTION_CLASS} min-w-0`}>
          <dl className="grid min-w-0 gap-3 text-sm">
            <div className="min-w-0">
              <dt className={CARD_INVARIANT_HEADING_CLASS}>Tillgänglighet</dt>
              <dd className={CARD_VALUE_CLASS}>
                {displayCardText(candidate.availabilityStatus)}
              </dd>
            </div>
            <div className="min-w-0">
              <dt className={CARD_INVARIANT_HEADING_CLASS}>Plats</dt>
              <dd className={CARD_VALUE_CLASS}>
                {displayCardText(candidate.country)} |{" "}
                {displayCardText(candidate.locationFlexibility)}
              </dd>
            </div>
          </dl>
        </section>

        <section className={`${MISSION_TOP_SECTION_CLASS} min-w-0`}>
          <dl className="grid min-w-0 gap-3 text-sm">
            <div className="min-w-0">
              <dt className={CARD_INVARIANT_HEADING_CLASS}>Arbetsläge</dt>
              <dd className={CARD_VALUE_CLASS}>
                {formatWorkMode(
                  candidate.workMode,
                  CANDIDATE_APPLICATION_WORK_MODE_LABELS,
                )}
              </dd>
            </div>
          </dl>
        </section>
      </div>

      <div className="h-px bg-slate-400/50" />

      <div className="grid min-w-0 gap-4 md:grid-cols-2">
        <section>
          <h4 className={`${CARD_INVARIANT_HEADING_CLASS} mb-2`}>
            Kompetenser - Primära
          </h4>
          <MatchViewerSkillChips skills={primarySkills} emptyText="No primary skills" />
        </section>
        <section>
          <h4 className={`${CARD_INVARIANT_HEADING_CLASS} mb-2`}>
            Kompetenser - Sekundära
          </h4>
          <MatchViewerSkillChips
            skills={secondarySkills}
            emptyText="No secondary skills"
          />
        </section>
      </div>
    </article>
  );
}
