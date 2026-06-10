import {
  MISSION_PRESENTATION_FIELDS,
  MISSION_PRESENTATION_MAX_WORDS,
} from "~/missions/mission_presentation/missionPresentationFields";
import {
  MISSION_FORM_WORK_MODE_LABELS,
  workModeOptions,
} from "~/reference_data/workMode";
import {
  limitWords,
  normalizeDateInput,
  wordCount,
} from "~/shared/formHelpers";
import type {
  MissionPresentationInput,
  MissionProposalInput,
} from "~/missions/types";

type PreviewFieldClass = (...fieldPaths: string[]) => string;

type MissionIntakeCustomerSectionProps = {
  form: MissionProposalInput;
  previewFieldClass: PreviewFieldClass;
  updateField: <K extends keyof MissionProposalInput>(
    name: K,
    value: MissionProposalInput[K],
  ) => void;
};

export function MissionIntakeCustomerSection({
  form,
  previewFieldClass,
  updateField,
}: MissionIntakeCustomerSectionProps) {
  return (
    <section className="grid gap-4">
      <h2 className="text-section text-lg font-semibold">Uppdragsgivare</h2>

      <div className="grid gap-4 md:grid-cols-2">
        <label className="grid gap-2 text-sm">
          <span className="text-label font-medium">Namn</span>
          <input
            required
            type="text"
            className={`input rounded px-3 py-2 ${previewFieldClass("customerName")}`}
            value={form.customerName}
            onChange={(event) =>
              updateField("customerName", event.target.value)
            }
          />
        </label>

        <label className="grid gap-2 text-sm">
          <span className="text-label font-medium">Email</span>
          <input
            required
            type="email"
            className={`input rounded px-3 py-2 ${previewFieldClass("customerEmail")}`}
            value={form.customerEmail}
            onChange={(event) =>
              updateField("customerEmail", event.target.value)
            }
          />
        </label>
      </div>
    </section>
  );
}

type MissionIntakeBasicsSectionProps = MissionIntakeCustomerSectionProps & {
  missionTitleWords: number;
  missionTitleMaxWords: number;
};

export function MissionIntakeBasicsSection({
  form,
  missionTitleWords,
  missionTitleMaxWords,
  previewFieldClass,
  updateField,
}: MissionIntakeBasicsSectionProps) {
  return (
    <section className="grid gap-4">
      <h2 className="text-section text-lg font-semibold">Uppdrag</h2>

      <div className="grid gap-4">
        <label className="grid gap-2 text-sm">
          <span className="text-label font-medium">Uppdragstitel</span>
          <input
            required
            type="text"
            className={`input rounded px-3 py-2 ${previewFieldClass("missionTitle")}`}
            value={form.missionTitle}
            onChange={(event) =>
              updateField(
                "missionTitle",
                limitWords(event.target.value, missionTitleMaxWords),
              )
            }
          />
          <span className="text-soft justify-self-end text-xs">
            {missionTitleWords} / {missionTitleMaxWords} ord
          </span>
        </label>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <label className="grid gap-2 text-sm">
          <span className="text-label font-medium">Startdatum</span>
          <input
            required
            type="text"
            inputMode="numeric"
            maxLength={10}
            pattern="\d{4}-\d{2}-\d{2}"
            placeholder="åååå-mm-dd"
            title="Ange datum som åååå-mm-dd"
            className={`input rounded px-3 py-2 ${previewFieldClass("startDate")}`}
            value={form.startDate}
            onChange={(event) =>
              updateField("startDate", normalizeDateInput(event.target.value))
            }
          />
        </label>

        <label className="grid gap-2 text-sm">
          <span className="text-label font-medium">Slutdatum</span>
          <input
            required
            type="text"
            inputMode="numeric"
            maxLength={10}
            pattern="\d{4}-\d{2}-\d{2}"
            placeholder="åååå-mm-dd"
            title="Ange datum som åååå-mm-dd"
            className={`input rounded px-3 py-2 ${previewFieldClass("endDate")}`}
            value={form.endDate}
            onChange={(event) =>
              updateField("endDate", normalizeDateInput(event.target.value))
            }
          />
        </label>

        <label className="grid gap-2 text-sm">
          <span className="text-label font-medium">Arbetsläge</span>
          <select
            required
            className={`input rounded px-3 py-2 ${previewFieldClass("workMode")}`}
            value={form.workMode}
            onChange={(event) =>
              updateField(
                "workMode",
                event.target.value as MissionProposalInput["workMode"],
              )
            }
          >
            {workModeOptions(MISSION_FORM_WORK_MODE_LABELS).map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
      </div>
    </section>
  );
}

type MissionIntakePresentationSectionProps = {
  form: MissionProposalInput;
  updatePresentationField: (
    name: keyof MissionPresentationInput,
    value: string,
  ) => void;
};

export function MissionIntakePresentationSection({
  form,
  updatePresentationField,
}: MissionIntakePresentationSectionProps) {
  return (
    <section className="grid gap-4">
      <div>
        <h2 className="text-section text-lg font-semibold">
          Uppdragspresentation
        </h2>
        <p className="text-muted mt-1 max-w-3xl text-sm leading-6">
          Information som en konsult bör veta om uppdragsgivaren eller
          uppdraget.
        </p>
      </div>

      <div className="grid gap-4">
        {MISSION_PRESENTATION_FIELDS.map((field) => {
          const value = form.missionPresentation[field.key];
          return (
            <label key={field.key} className="grid gap-2 text-sm">
              <span className="text-label font-medium">{field.label}</span>
              <textarea
                required
                className="input min-h-24 rounded px-3 py-2 leading-6"
                value={value}
                onChange={(event) =>
                  updatePresentationField(field.key, event.target.value)
                }
              />
              <span className="text-soft justify-self-end text-xs">
                {wordCount(value)} / {MISSION_PRESENTATION_MAX_WORDS} ord
              </span>
            </label>
          );
        })}
      </div>
    </section>
  );
}

type MissionIntakeSubmitSectionProps = {
  error: string | null;
  receiptId: number | null;
  contactEmail: string;
  submitting: boolean;
  loadingReferences: boolean;
};

export function MissionIntakeSubmitSection({
  error,
  receiptId,
  contactEmail,
  submitting,
  loadingReferences,
}: MissionIntakeSubmitSectionProps) {
  return (
    <>
      {error && <p className="text-danger text-sm">{error}</p>}
      {receiptId && (
        <div className="grid max-w-3xl gap-4 rounded-xl border border-cyan-400/20 bg-cyan-400/5 p-4">
          <p className="text-success text-lg font-semibold leading-7">
            Uppdraget är mottaget för granskning. Detta uppdrag har referens: #
            {receiptId}.
          </p>
          <p className="text-section text-sm leading-6">
            För att ändra uppdragsuppgifter: kontakta admin via{" "}
            <a
              className="text-link font-semibold"
              href={`mailto:${contactEmail}`}
            >
              {contactEmail}
            </a>
          </p>
        </div>
      )}

      <button
        type="submit"
        disabled={submitting || loadingReferences}
        className="btn btn-main justify-self-start rounded px-5 py-2 text-sm font-semibold"
      >
        {submitting ? "Skickar..." : "Skicka förfrågan"}
      </button>
    </>
  );
}
