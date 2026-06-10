import type {
  MissionProposalInput,
  MissionProposalPreviewView,
} from "~/missions/types";
import { MISSION_SUMMARY_WORK_MODE_LABELS } from "~/reference_data/workMode";

export type MissionIntakePreviewSkillRow = {
  key: string;
  slotLabel: string;
  title: string;
  level: string;
};

type MissionIntakePreviewPanelProps = {
  form: MissionProposalInput;
  roleRequirementsText: string;
  roleRequirementsWords: number;
  roleRequirementsMaxWords: number;
  previewingRequirements: boolean;
  loadingReferences: boolean;
  requirementsPreview: MissionProposalPreviewView | null;
  requirementsPreviewError: string | null;
  primarySkillRows: MissionIntakePreviewSkillRow[];
  secondarySkillRows: MissionIntakePreviewSkillRow[];
  roleTitle: (roleId: number) => string;
  onRoleRequirementsTextChange: (value: string) => void;
  onPreviewRoleRequirements: () => void;
};

export function MissionIntakePreviewPanel({
  form,
  roleRequirementsText,
  roleRequirementsWords,
  roleRequirementsMaxWords,
  previewingRequirements,
  loadingReferences,
  requirementsPreview,
  requirementsPreviewError,
  primarySkillRows,
  secondarySkillRows,
  roleTitle,
  onRoleRequirementsTextChange,
  onPreviewRoleRequirements,
}: MissionIntakePreviewPanelProps) {
  return (
    <section className="panel-soft grid gap-4 rounded-xl p-5 text-sm">
      <div className="min-w-0">
        <h2 className="text-section text-lg font-semibold">
          Uppdragsbeskrivning
        </h2>
        <p className="text-muted mt-1 max-w-3xl break-words leading-6">
          Översiktlig beskrivning av uppdraget.
        </p>
      </div>

      <label className="grid gap-2">
        <div className="flex items-center justify-between gap-4">
          <span className="text-label font-medium">Beskriv uppdraget</span>
          <span className="text-soft text-xs">
            {roleRequirementsWords} / {roleRequirementsMaxWords} ord
          </span>
        </div>
        <textarea
          className="input min-h-36 rounded px-3 py-2"
          value={roleRequirementsText}
          placeholder="Exempel: Vi behöver två seniora Java-utvecklare med Spring Framework, PostgreSQL och hybridarbete i Stockholm från september."
          onChange={(event) =>
            onRoleRequirementsTextChange(event.target.value)
          }
        />
      </label>

      <p className="text-muted text-sm">
        Tryck på Autofyll nedan för att fylla i fält baserat på
        uppdragsbeskrivningen, eller fyll i fälten manuellt.
      </p>

      <div className="flex flex-wrap justify-start gap-2">
        <button
          type="button"
          className="btn btn-soft rounded px-4 py-2 text-sm font-semibold disabled:opacity-50"
          onClick={onPreviewRoleRequirements}
          disabled={previewingRequirements || loadingReferences}
        >
          {previewingRequirements ? "Tolkar..." : "Autofyll"}
        </button>
      </div>

      {requirementsPreviewError && (
        <p className="text-danger text-sm">{requirementsPreviewError}</p>
      )}

      {requirementsPreview && (
        <div className="grid gap-4">
          <div className="grid gap-3 rounded-xl border border-cyan-400/20 bg-cyan-400/5 p-4">
            <h3 className="text-label font-semibold">
              Autofyll-förhandsvisning
            </h3>
            <div className="grid gap-2 text-sm">
              <p className="text-muted">
                Mission:{" "}
                <span className="text-section">
                  {form.missionTitle.trim() || "Mission-namn saknas"}
                </span>
              </p>
              <p className="text-muted">
                Period:{" "}
                <span className="text-section">
                  {form.startDate || "Startdatum ej angivet"} -{" "}
                  {form.endDate || "(Slutdatum ej angivet)"}
                </span>
              </p>
              <p className="text-muted">
                Arbetsläge:{" "}
                <span className="text-section">
                  {MISSION_SUMMARY_WORK_MODE_LABELS[form.workMode]}
                </span>
              </p>
            </div>

            <div className="grid gap-3 md:grid-cols-3">
              <section className="grid gap-2 rounded-lg border border-white/10 p-3">
                <h4 className="text-label text-xs font-semibold uppercase">
                  Roller
                </h4>
                {form.missionSlots.map((slot, slotIndex) => (
                  <p
                    key={`preview-role-${slotIndex}`}
                    className="text-muted text-sm"
                  >
                    <span className="text-section">
                      Slot {slotIndex + 1}:
                    </span>{" "}
                    {roleTitle(slot.roleId)} / Mer än{" "}
                    {slot.requiredRoleExperienceYears} år
                  </p>
                ))}
              </section>

              <MissionIntakeSkillPreviewColumn
                title="Primary Skills"
                emptyText="No primary skills in preview."
                rows={primarySkillRows}
              />

              <MissionIntakeSkillPreviewColumn
                title="Secondary Skills"
                emptyText="No secondary skills in preview."
                rows={secondarySkillRows}
              />
            </div>
          </div>
        </div>
      )}
    </section>
  );
}

function MissionIntakeSkillPreviewColumn({
  title,
  emptyText,
  rows,
}: {
  title: string;
  emptyText: string;
  rows: MissionIntakePreviewSkillRow[];
}) {
  return (
    <section className="grid gap-2 rounded-lg border border-white/10 p-3">
      <h4 className="text-label text-xs font-semibold uppercase">{title}</h4>
      {rows.length === 0 ? (
        <p className="text-soft text-xs">{emptyText}</p>
      ) : (
        rows.map((skill) => (
          <p key={skill.key} className="text-muted text-sm">
            <span className="text-section">{skill.slotLabel}:</span>{" "}
            {skill.title} / {skill.level}
          </p>
        ))
      )}
    </section>
  );
}
