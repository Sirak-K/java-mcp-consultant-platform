import { useSearchParams } from "react-router";
import { ErrorPlaceholder, LoadingPlaceholder } from "~/shared/components";
import { candidatePresentationApiPaths } from "~/candidate_presentation/api/candidatePresentationApi";
import { CandidatePresentationArtifactDetails } from "./CandidatePresentationArtifactDetails";
import { CandidatePresentationArtifactsList } from "./CandidatePresentationArtifactsList";
import { useCandidatePresentationArtifacts } from "./useCandidatePresentationArtifacts";

const contract = [
  `GET ${candidatePresentationApiPaths.artifacts} -> CandidatePresentationArtifactView[]`,
  `GET ${candidatePresentationApiPaths.artifact("{id}")} -> CandidatePresentationArtifactView`,
  `PUT ${candidatePresentationApiPaths.artifact("{id}")} -> CandidatePresentationArtifactView`,
];

export default function CandidatePresentationArtifactsPage() {
  const [searchParams] = useSearchParams();
  const {
    actionError,
    artifactVersionMode,
    artifacts,
    canEditSelectedArtifact,
    cancelEditing,
    editJsonValid,
    editMode,
    editState,
    error,
    loadArtifacts,
    loading,
    pollingArtifactId,
    saveSelectedArtifact,
    saving,
    selectArtifact,
    selectedArtifact,
    setArtifactVersionMode,
    startEditing,
    updateEditState,
  } = useCandidatePresentationArtifacts(searchParams);

  if (loading) return <LoadingPlaceholder />;

  return (
    <div className="mx-auto grid w-full max-w-7xl gap-6 p-6">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <div className="max-w-4xl">
          <p className="text-label text-xs font-semibold uppercase tracking-[0.18em]">
            Lista på Genererade Kandidatpresentationer
          </p>
          <h1 className="text-title mt-2 text-2xl font-semibold">
            Kandidatpresentationer
          </h1>
          <p className="text-muted mt-2 text-sm leading-6">
            Granska, redigera eller vidarebefordra till kunder.
          </p>
        </div>
        <button
          type="button"
          className="btn btn-soft rounded px-4 py-2 text-sm font-medium"
          disabled={loading || saving}
          onClick={() => void loadArtifacts(selectedArtifact?.id ?? null)}
        >
          Uppdatera
        </button>
      </header>

      {error && (
        <div>
          <ErrorPlaceholder message={error} />
          <div className="panel mt-4 rounded p-4 text-sm">
            <h2 className="text-section mb-2 font-medium">
              Förväntat API-kontrakt
            </h2>
            <div className="text-muted grid gap-1">
              {contract.map((line) => (
                <p key={line}>{line}</p>
              ))}
            </div>
          </div>
        </div>
      )}

      {!error && artifacts.length === 0 && (
        <div className="panel rounded p-6 text-sm">
          <p className="text-soft">
            Det finns inga kandidatpresentationer ännu.
          </p>
        </div>
      )}

      {!error && artifacts.length > 0 && (
        <div className="grid gap-5 xl:grid-cols-[360px_1fr]">
          <CandidatePresentationArtifactsList
            artifacts={artifacts}
            selectedArtifactId={selectedArtifact?.id ?? null}
            onSelectArtifact={selectArtifact}
          />

          {selectedArtifact && (
            <CandidatePresentationArtifactDetails
              actionError={actionError}
              artifact={selectedArtifact}
              artifactVersionMode={artifactVersionMode}
              canEditSelectedArtifact={canEditSelectedArtifact}
              editJsonValid={editJsonValid}
              editMode={editMode}
              editState={editState}
              pollingArtifactId={pollingArtifactId}
              saving={saving}
              onArtifactVersionModeChange={setArtifactVersionMode}
              onCancelEditing={cancelEditing}
              onSave={() => void saveSelectedArtifact()}
              onStartEditing={startEditing}
              onUpdateEditField={updateEditState}
            />
          )}
        </div>
      )}
    </div>
  );
}
