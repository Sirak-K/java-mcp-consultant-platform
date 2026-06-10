import { formatCompetencyLevelLabel } from "~/reference_data/competencyLevels";
import {
  MISSION_PRESENTATION_DEFAULTS,
  MISSION_PRESENTATION_FIELDS,
} from "~/missions/mission_presentation/missionPresentationFields";
import { formatWorkMode } from "~/reference_data/workMode";
import type { MissionProposalView } from "~/missions/types";

const invariantHeadingClass =
  "text-label text-lg font-semibold underline underline-offset-4 decoration-1";

type MissionProposalReviewSummaryProps = {
  item: MissionProposalView;
};

export function MissionProposalReviewSummary({
  item,
}: MissionProposalReviewSummaryProps) {
  return (
<dl className="grid gap-5">
  <div className="grid gap-1">
    <dt className={invariantHeadingClass}>Uppdragsgivarens namn</dt>
    <dd>{item.specification.customerName}</dd>
  </div>
  <div className="grid gap-1">
    <dt className={invariantHeadingClass}>Kundens e-post</dt>
    <dd>{item.specification.customerEmail}</dd>
  </div>
  <div className="grid gap-1">
    <dt className={invariantHeadingClass}>Uppdragstitel</dt>
    <dd>{item.specification.missionTitle}</dd>
  </div>
  <div className="grid gap-1">
    <dt className={invariantHeadingClass}>Period</dt>
    <dd>
      {item.specification.startDate} till {item.specification.endDate}
    </dd>
  </div>
  <div className="grid gap-1">
    <dt className={invariantHeadingClass}>Arbetsläge</dt>
    <dd>{formatWorkMode(item.specification.workMode)}</dd>
  </div>
  <div className="grid gap-1">
    <dt className={invariantHeadingClass}>Uppdragspositioner</dt>
    <dd className="grid gap-2">
      {item.specification.missionSlots.map((slot) => (
        <span key={slot.slotNumber}>
          Position {slot.slotNumber}: {slot.roleTitle},{" "}
          {slot.requiredRoleExperienceYears} år. Kompetenser:{" "}
          {slot.requiredSkills
            .map(
              (skill) =>
                `${skill.skillTitle} (${formatCompetencyLevelLabel(skill.skillLevelName)})`,
            )
            .join(", ")}
        </span>
      ))}
    </dd>
  </div>
  <div className="grid gap-3">
    <dt className={invariantHeadingClass}>Uppdragspresentation</dt>
    <dd className="grid gap-3">
      {MISSION_PRESENTATION_FIELDS.map((field) => (
        <section key={field.key} className="grid gap-1">
          <h4 className="text-label text-sm font-semibold">
            {field.label}
          </h4>
          <p className="text-muted leading-6">
            {item.specification.missionPresentation?.[
              field.key
            ] ??
              MISSION_PRESENTATION_DEFAULTS[field.key]}
          </p>
        </section>
      ))}
    </dd>
  </div>

  <div className="grid gap-1">
    <dt className={invariantHeadingClass}>Extraktionsunderlag</dt>
    {(item.evidence ?? []).length === 0 ? (
      <dd className="text-soft">
        {item.evidence === undefined
          ? "No parser evidence metadata available in this response."
          : "No parser evidence returned."}
      </dd>
    ) : (
      <dd className="grid gap-3">
        {(item.evidence ?? []).map((evidenceItem, index) => (
          <div
            key={`${evidenceItem.field}-${index}`}
            className="grid gap-1"
          >
            <span>
              {evidenceItem.field}: {evidenceItem.value}
            </span>
            <span className="text-soft break-words text-xs">
              {evidenceItem.sourceText}
            </span>
          </div>
        ))}
      </dd>
    )}
  </div>
  <div className="grid gap-1">
    <dt className={invariantHeadingClass}>Saknade fält</dt>
    {(item.missingFields ?? []).length === 0 ? (
      <dd className="text-soft">
        {item.missingFields === undefined
          ? "No missing-field metadata available in this response."
          : "No missing fields returned."}
      </dd>
    ) : (
      <dd className="flex flex-wrap gap-2">
        {(item.missingFields ?? []).map((field) => (
          <span
            key={field}
            className="rounded-full border border-amber-300/35 bg-amber-300/10 px-3 py-1 text-xs text-amber-100"
          >
            {field}
          </span>
        ))}
      </dd>
    )}
  </div>
</dl>
  );
}
