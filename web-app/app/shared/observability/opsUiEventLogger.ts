import { apiClient } from "~/shared/api/client";

export type OpsUiEventDetails = Record<
  string,
  string | number | boolean | null | undefined
>;

export type OpsUiEventName =
  | "match_viewer_candidate_presentation_generation_clicked"
  | "candidate_presentation_generation_start_api_accepted"
  | "candidate_presentation_generation_start_api_blocked"
  | "candidate_presentation_generation_start_api_failed"
  | "candidate_presentation_tab_navigated"
  | "candidate_presentation_status_displayed"
  | "candidate_presentation_edit_clicked"
  | "candidate_presentation_save_clicked"
  | "candidate_presentation_save_api_succeeded"
  | "candidate_presentation_save_api_failed"
  | "candidate_presentation_list_api_succeeded"
  | "candidate_presentation_list_api_failed";

export interface OpsUiEvent {
  eventName: OpsUiEventName;
  route: string;
  details?: OpsUiEventDetails;
}

const opsUiEventLogPath = "/api/ops/react-app-log";

function compactDetails(
  details: OpsUiEventDetails | undefined,
): OpsUiEventDetails | undefined {
  if (!details) {
    return undefined;
  }

  return Object.fromEntries(
    Object.entries(details).filter(([, value]) => value !== undefined),
  ) as OpsUiEventDetails;
}

export const opsUiEventLogger = {
  log(event: OpsUiEvent): void {
    const payload: OpsUiEvent = {
      eventName: event.eventName,
      route: event.route,
      details: compactDetails(event.details),
    };

    void apiClient.post<void>(opsUiEventLogPath, payload).catch(() => {
      // Operator UI tracking must never block or surface errors in the operator flow.
    });
  },
};
