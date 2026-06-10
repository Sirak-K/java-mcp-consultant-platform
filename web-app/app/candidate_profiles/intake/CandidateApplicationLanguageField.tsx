import {
  parseLabelValues,
  type CandidateCvProfileFormFields,
} from "~/candidate_profiles/intake/candidateApplicationIntakeState";

type CandidateApplicationLanguageFieldProps = {
  value: CandidateCvProfileFormFields["languages"];
  workingCopyValue: string;
  onWorkingCopyChange: (value: string) => void;
  onCommit: () => void;
  onRemove: (value: string) => void;
};

export function CandidateApplicationLanguageField({
  value,
  workingCopyValue,
  onWorkingCopyChange,
  onCommit,
  onRemove,
}: CandidateApplicationLanguageFieldProps) {
  const labels = parseLabelValues(value);

  return (
    <label className="grid gap-2 md:col-span-2">
      <span className="text-label font-medium">Språk</span>
      <div className="flex flex-wrap items-center gap-3">
        <input
          className="input min-w-[16rem] flex-1 rounded px-3 py-2"
          value={workingCopyValue}
          placeholder="Lägg till ett språk"
          onChange={(event) => onWorkingCopyChange(event.target.value)}
          onBlur={onCommit}
          onKeyDown={(event) => {
            if (event.key === "Enter" || event.key === ",") {
              event.preventDefault();
              onCommit();
            }
          }}
        />
        <button
          type="button"
          onClick={onCommit}
          className="btn btn-soft rounded px-4 py-2 text-xs font-semibold"
        >
          Lägg till Språk
        </button>
      </div>
      <div className="flex flex-wrap gap-2">
        {labels.length === 0 ? (
          <span className="text-soft text-xs">Inga språk tillagda.</span>
        ) : (
          labels.map((item) => (
            <button
              key={item}
              type="button"
              onClick={() => onRemove(item)}
              className="inline-flex items-center gap-2 rounded-full border border-cyan-400/35 bg-cyan-400/10 px-3 py-1 text-xs font-medium text-cyan-100 transition hover:border-cyan-300/50 hover:bg-cyan-400/15"
            >
              <span>{item}</span>
              <span className="text-cyan-200/80">×</span>
            </button>
          ))
        )}
      </div>
    </label>
  );
}
