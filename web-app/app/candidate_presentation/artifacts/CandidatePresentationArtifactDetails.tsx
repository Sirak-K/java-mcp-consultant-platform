import type { CandidatePresentationArtifactEditInput, CandidatePresentationArtifactView } from "../types";
import {
  artifactEditActionTitle,
  artifactStatusClass,
  artifactStatusDescription,
  artifactStatusLabel,
  artifactSubtitle,
  displayArtifactValue,
  presentationTitleForMatch,
} from "./candidatePresentationArtifactDisplay";
import { CandidatePresentationArtifactEditForm } from "./CandidatePresentationArtifactEditForm";
import { CandidatePresentationArtifactPreviews } from "./CandidatePresentationArtifactPreviews";
import type { CandidatePresentationArtifactVersionMode } from "./useCandidatePresentationArtifacts";

interface CandidatePresentationArtifactDetailsProps {
  actionError: string | null;
  artifact: CandidatePresentationArtifactView;
  artifactVersionMode: CandidatePresentationArtifactVersionMode;
  canEditSelectedArtifact: boolean;
  editJsonValid: boolean;
  editMode: boolean;
  editState: CandidatePresentationArtifactEditInput | null;
  pollingArtifactId: number | null;
  saving: boolean;
  onArtifactVersionModeChange: (
    mode: CandidatePresentationArtifactVersionMode,
  ) => void;
  onCancelEditing: () => void;
  onSave: () => void;
  onStartEditing: () => void;
  onUpdateEditField: (
    field: keyof CandidatePresentationArtifactEditInput,
    value: string,
  ) => void;
}

function DetailRow({
  label,
  value,
}: {
  label: string;
  value: string | number | null | undefined;
}) {
  return (
    <div>
      <p className="text-label text-xs font-semibold uppercase tracking-wide">
        {label}
      </p>
      <p className="text-muted text-sm">{displayArtifactValue(value)}</p>
    </div>
  );
}

export function CandidatePresentationArtifactDetails({
  actionError,
  artifact,
  artifactVersionMode,
  canEditSelectedArtifact,
  editJsonValid,
  editMode,
  editState,
  pollingArtifactId,
  saving,
  onArtifactVersionModeChange,
  onCancelEditing,
  onSave,
  onStartEditing,
  onUpdateEditField,
}: CandidatePresentationArtifactDetailsProps) {
  return (
    <article className="panel rounded p-5 md:p-7">
      <header className="grid gap-5 border-b border-cyan-400/20 pb-5 lg:grid-cols-[1fr_auto]">
        <div>
          <p className="text-label text-xs font-semibold uppercase tracking-[0.18em]">
            Kandidatpresentation
          </p>
          <h2 className="text-title mt-2 text-2xl font-semibold">
            {presentationTitleForMatch(artifact.sourceCandidateToSlotMatchId)}
          </h2>
          <p className="text-muted mt-2 text-sm">{artifactSubtitle(artifact)}</p>
        </div>
        <div className="flex flex-wrap items-start gap-2">
          {!editMode && (
            <button
              type="button"
              className="btn btn-soft rounded px-4 py-2 text-sm font-medium disabled:cursor-not-allowed disabled:opacity-60"
              disabled={saving || !canEditSelectedArtifact}
              title={artifactEditActionTitle(artifact.artifactStatus)}
              onClick={onStartEditing}
            >
              Redigera
            </button>
          )}
          {editMode && (
            <>
              <button
                type="button"
                className="btn btn-main rounded px-4 py-2 text-sm font-medium"
                disabled={saving || !editJsonValid || !canEditSelectedArtifact}
                onClick={onSave}
              >
                {saving ? "Sparar..." : "Spara"}
              </button>
              <button
                type="button"
                className="btn btn-soft rounded px-4 py-2 text-sm font-medium"
                disabled={saving}
                onClick={onCancelEditing}
              >
                Avbryt
              </button>
            </>
          )}
        </div>
      </header>

      {actionError && (
        <div className="panel-soft mt-5 rounded p-4 text-sm">
          <span className="text-danger">{actionError}</span>
        </div>
      )}

      <div
        className={`mt-5 rounded border px-4 py-3 text-sm ${artifactStatusClass(artifact.artifactStatus)}`}
      >
        <p className="font-semibold">
          {artifactStatusLabel(artifact.artifactStatus)}
        </p>
        <p className="mt-1 opacity-90">
          {artifactStatusDescription(artifact.artifactStatus)}
        </p>
        {artifact.artifactStatus === "PENDING_GENERATION" && (
          <p className="mt-2 text-xs opacity-80">
            {pollingArtifactId === artifact.id
              ? "Uppdaterar genereringsstatus..."
              : "Genereringsstatus uppdateras automatiskt."}
          </p>
        )}
      </div>

      <div className="mt-6 grid gap-5 md:grid-cols-4">
        <DetailRow label="Artefakt-ID" value={artifact.id} />
        <DetailRow label="Status" value={artifactStatusLabel(artifact.artifactStatus)} />
        <DetailRow label="Uppdaterad" value={artifact.updatedAt} />
      </div>

      {editMode && editState ? (
        <CandidatePresentationArtifactEditForm
          editState={editState}
          onUpdateField={onUpdateEditField}
        />
      ) : (
        <CandidatePresentationArtifactPreviews
          artifact={artifact}
          artifactVersionMode={artifactVersionMode}
          onArtifactVersionModeChange={onArtifactVersionModeChange}
        />
      )}
    </article>
  );
}
