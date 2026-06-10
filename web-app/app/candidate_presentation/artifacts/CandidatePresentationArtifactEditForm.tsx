import type { CandidatePresentationArtifactEditInput } from "../types";
import { isValidArtifactJson } from "./candidatePresentationArtifactContent";

interface CandidatePresentationArtifactEditFormProps {
  editState: CandidatePresentationArtifactEditInput;
  onUpdateField: (
    field: keyof CandidatePresentationArtifactEditInput,
    value: string,
  ) => void;
}

function JsonTextarea({
  label,
  value,
  onChange,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
}) {
  const valid = isValidArtifactJson(value);
  return (
    <label className="grid gap-2">
      <span className="text-label text-sm font-semibold">{label}</span>
      <textarea
        className={`input min-h-44 rounded p-3 font-mono text-xs ${valid ? "" : "border-red-400/70"}`}
        value={value}
        onChange={(event) => onChange(event.target.value)}
      />
      {!valid && <span className="text-danger text-xs">Ogiltig JSON</span>}
    </label>
  );
}

export function CandidatePresentationArtifactEditForm({
  editState,
  onUpdateField,
}: CandidatePresentationArtifactEditFormProps) {
  return (
    <div className="mt-7 grid gap-5">
      <JsonTextarea
        label="Kundversion JSON"
        value={editState.customerFacingContentJson}
        onChange={(value) => onUpdateField("customerFacingContentJson", value)}
      />
      <JsonTextarea
        label="OPS-granskning JSON"
        value={editState.opsReviewContentJson}
        onChange={(value) => onUpdateField("opsReviewContentJson", value)}
      />
      <JsonTextarea
        label="Evidensspår JSON"
        value={editState.evidenceTraceJson}
        onChange={(value) => onUpdateField("evidenceTraceJson", value)}
      />
    </div>
  );
}
