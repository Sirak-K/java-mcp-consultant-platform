export type WorkMode = "ON_PREMISE" | "REMOTE" | "HYBRID";

export type WorkModeLabelMap = Record<WorkMode, string>;

export const SWEDISH_WORK_MODE_LABELS: WorkModeLabelMap = {
  REMOTE: "Distans",
  HYBRID: "Hybrid",
  ON_PREMISE: "På plats",
};

export const MISSION_FORM_WORK_MODE_LABELS: WorkModeLabelMap =
  SWEDISH_WORK_MODE_LABELS;

export const MISSION_SUMMARY_WORK_MODE_LABELS: WorkModeLabelMap =
  SWEDISH_WORK_MODE_LABELS;

export const CANDIDATE_APPLICATION_WORK_MODE_LABELS: WorkModeLabelMap =
  SWEDISH_WORK_MODE_LABELS;

export const CANDIDATE_PROFILE_REVIEW_WORK_MODE_LABELS: WorkModeLabelMap =
  SWEDISH_WORK_MODE_LABELS;

export function workModeOptions(labels: WorkModeLabelMap): Array<{
  value: WorkMode;
  label: string;
}> {
  return [
    { value: "REMOTE", label: labels.REMOTE },
    { value: "HYBRID", label: labels.HYBRID },
    { value: "ON_PREMISE", label: labels.ON_PREMISE },
  ];
}

export function isWorkMode(value: string): value is WorkMode {
  return value === "ON_PREMISE" || value === "REMOTE" || value === "HYBRID";
}

export function workModeForEdit(value: string): WorkMode {
  return isWorkMode(value) ? value : "REMOTE";
}

export function formatWorkMode(
  value: string | null | undefined,
  labels: WorkModeLabelMap = SWEDISH_WORK_MODE_LABELS,
): string {
  if (!value) {
    return "N/A";
  }
  if (isWorkMode(value)) {
    return labels[value];
  }
  return formatWorkModeText(value.replace(/_/g, " "));
}

export function formatWorkModeText(value: string): string {
  return value
    .replace(/\bON_PREMISE\b/gi, SWEDISH_WORK_MODE_LABELS.ON_PREMISE)
    .replace(/\bREMOTE\b/gi, SWEDISH_WORK_MODE_LABELS.REMOTE)
    .replace(/\bHYBRID\b/gi, SWEDISH_WORK_MODE_LABELS.HYBRID)
    .replace(/\bOn premise\b/gi, SWEDISH_WORK_MODE_LABELS.ON_PREMISE)
    .replace(/\bRemote\b/gi, SWEDISH_WORK_MODE_LABELS.REMOTE);
}
