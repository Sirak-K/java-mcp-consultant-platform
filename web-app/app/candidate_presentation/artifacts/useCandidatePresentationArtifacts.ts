import { useCallback, useEffect, useMemo, useState } from "react";
import { candidatePresentationApi } from "~/candidate_presentation/api/candidatePresentationApi";
import type {
  CandidatePresentationArtifactEditInput,
  CandidatePresentationArtifactView,
} from "~/candidate_presentation/types";
import type { ApiError } from "~/shared/api/apiErrors";
import { opsUiEventLogger } from "~/shared/observability/opsUiEventLogger";
import {
  buildArtifactEditState,
  isValidArtifactJson,
} from "./candidatePresentationArtifactContent";
import {
  canEditArtifactStatus,
  shouldLogDisplayedArtifactStatus,
} from "./candidatePresentationArtifactDisplay";

export type CandidatePresentationArtifactVersionMode = "customer" | "ops";

const GENERATION_STATUS_POLL_INTERVAL_MS = 2500;
const routePath = "/ops/candidate-presentation-artifacts";

export function useCandidatePresentationArtifacts(
  searchParams: URLSearchParams,
) {
  const [artifacts, setArtifacts] = useState<
    CandidatePresentationArtifactView[]
  >([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [editMode, setEditMode] = useState(false);
  const [editState, setEditState] =
    useState<CandidatePresentationArtifactEditInput | null>(null);
  const [artifactVersionMode, setArtifactVersionMode] =
    useState<CandidatePresentationArtifactVersionMode>("customer");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [pollingArtifactId, setPollingArtifactId] = useState<number | null>(
    null,
  );
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const selectedArtifact = useMemo(() => {
    if (selectedId === null) return artifacts[0] ?? null;
    return (
      artifacts.find((artifact) => artifact.id === selectedId) ??
      artifacts[0] ??
      null
    );
  }, [artifacts, selectedId]);

  const editJsonValid = editState
    ? isValidArtifactJson(editState.customerFacingContentJson) &&
      isValidArtifactJson(editState.opsReviewContentJson) &&
      isValidArtifactJson(editState.evidenceTraceJson)
    : false;

  const canEditSelectedArtifact = selectedArtifact
    ? canEditArtifactStatus(selectedArtifact.artifactStatus)
    : false;

  const loadArtifacts = useCallback(async (preferredId?: number | null) => {
    setLoading(true);
    setError(null);
    try {
      const result = await candidatePresentationApi.listArtifacts();
      opsUiEventLogger.log({
        eventName: "candidate_presentation_list_api_succeeded",
        route: routePath,
        details: {
          artifactCount: result.length,
          preferredArtifactId: preferredId ?? null,
        },
      });
      setArtifacts(result);
      const preferredExists =
        preferredId !== null &&
        preferredId !== undefined &&
        result.some((artifact) => artifact.id === preferredId);
      setSelectedId(preferredExists ? preferredId : (result[0]?.id ?? null));
    } catch (err) {
      const apiError = err as ApiError;
      opsUiEventLogger.log({
        eventName: "candidate_presentation_list_api_failed",
        route: routePath,
        details: {
          preferredArtifactId: preferredId ?? null,
          errorCode: apiError.code,
          status: apiError.status ?? null,
        },
      });
      setArtifacts([]);
      setSelectedId(null);
      setError(
        apiError.message ?? "Kunde inte hämta kandidatpresentationer.",
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const artifactId = Number(searchParams.get("artifactId"));
    opsUiEventLogger.log({
      eventName: "candidate_presentation_tab_navigated",
      route: routePath,
      details: { artifactId: Number.isFinite(artifactId) ? artifactId : null },
    });
    void loadArtifacts(Number.isFinite(artifactId) ? artifactId : null);
  }, [loadArtifacts, searchParams]);

  useEffect(() => {
    if (!selectedArtifact || !editMode) return;
    setEditState(buildArtifactEditState(selectedArtifact));
  }, [selectedArtifact, editMode]);

  useEffect(() => {
    if (
      !selectedArtifact ||
      !shouldLogDisplayedArtifactStatus(selectedArtifact.artifactStatus)
    )
      return;
    opsUiEventLogger.log({
      eventName: "candidate_presentation_status_displayed",
      route: routePath,
      details: {
        artifactId: selectedArtifact.id,
        artifactStatus: selectedArtifact.artifactStatus,
      },
    });
  }, [selectedArtifact?.id, selectedArtifact?.artifactStatus]);

  useEffect(() => {
    if (
      !selectedArtifact ||
      selectedArtifact.artifactStatus !== "PENDING_GENERATION" ||
      editMode
    ) {
      setPollingArtifactId(null);
      return;
    }

    let active = true;
    let refreshing = false;
    const artifactId = selectedArtifact.id;

    async function refreshSelectedArtifact() {
      if (refreshing) return;
      refreshing = true;
      setPollingArtifactId(artifactId);
      try {
        const refreshed = await candidatePresentationApi.artifact(artifactId);
        if (!active) return;
        setArtifacts((current) => {
          const exists = current.some(
            (artifact) => artifact.id === refreshed.id,
          );
          if (!exists) return [refreshed, ...current];
          return current.map((artifact) =>
            artifact.id === refreshed.id ? refreshed : artifact,
          );
        });
        setSelectedId(refreshed.id);
        if (refreshed.artifactStatus !== "PENDING_GENERATION") {
          setActionError(null);
        }
      } catch (err) {
        if (!active) return;
        const apiError = err as ApiError;
        setActionError(
          apiError.message ??
            "Kunde inte uppdatera genereringsstatus för kandidatpresentationen.",
        );
      } finally {
        refreshing = false;
        if (active) setPollingArtifactId(null);
      }
    }

    void refreshSelectedArtifact();
    const intervalId = window.setInterval(
      () => void refreshSelectedArtifact(),
      GENERATION_STATUS_POLL_INTERVAL_MS,
    );

    return () => {
      active = false;
      window.clearInterval(intervalId);
    };
  }, [selectedArtifact?.id, selectedArtifact?.artifactStatus, editMode]);

  function selectArtifact(artifact: CandidatePresentationArtifactView) {
    setSelectedId(artifact.id);
    setEditMode(false);
    setEditState(null);
    setArtifactVersionMode("customer");
    setActionError(null);
  }

  function startEditing() {
    if (
      !selectedArtifact ||
      !canEditArtifactStatus(selectedArtifact.artifactStatus)
    )
      return;
    opsUiEventLogger.log({
      eventName: "candidate_presentation_edit_clicked",
      route: routePath,
      details: {
        artifactId: selectedArtifact.id,
        artifactStatus: selectedArtifact.artifactStatus,
      },
    });
    setEditMode(true);
    setEditState(buildArtifactEditState(selectedArtifact));
    setActionError(null);
  }

  function cancelEditing() {
    setEditMode(false);
    setEditState(null);
    setActionError(null);
  }

  function updateEditState(
    field: keyof CandidatePresentationArtifactEditInput,
    value: string,
  ) {
    setEditState((current) =>
      current ? { ...current, [field]: value } : current,
    );
  }

  async function saveSelectedArtifact() {
    if (
      !selectedArtifact ||
      !editState ||
      !editJsonValid ||
      !canEditArtifactStatus(selectedArtifact.artifactStatus)
    )
      return;
    opsUiEventLogger.log({
      eventName: "candidate_presentation_save_clicked",
      route: routePath,
      details: {
        artifactId: selectedArtifact.id,
        artifactStatus: selectedArtifact.artifactStatus,
      },
    });
    setSaving(true);
    setActionError(null);
    try {
      const saved = await candidatePresentationApi.editArtifact(
        selectedArtifact.id,
        { ...editState },
      );
      opsUiEventLogger.log({
        eventName: "candidate_presentation_save_api_succeeded",
        route: routePath,
        details: {
          artifactId: saved.id,
          artifactStatus: saved.artifactStatus,
        },
      });
      setArtifacts((current) =>
        current.map((artifact) =>
          artifact.id === saved.id ? saved : artifact,
        ),
      );
      setSelectedId(saved.id);
      setEditMode(false);
      setEditState(null);
    } catch (err) {
      const apiError = err as ApiError;
      opsUiEventLogger.log({
        eventName: "candidate_presentation_save_api_failed",
        route: routePath,
        details: {
          artifactId: selectedArtifact.id,
          artifactStatus: selectedArtifact.artifactStatus,
          errorCode: apiError.code,
          status: apiError.status ?? null,
        },
      });
      setActionError(
        apiError.message ?? "Kunde inte spara kandidatpresentationen.",
      );
    } finally {
      setSaving(false);
    }
  }

  return {
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
  };
}
