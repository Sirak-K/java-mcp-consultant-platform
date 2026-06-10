export type ApiErrorCode =
  | "BAD_REQUEST"
  | "UNAUTHORIZED"
  | "FORBIDDEN"
  | "NOT_FOUND"
  | "CONFLICT"
  | "SERVER_ERROR"
  | "NETWORK_ERROR";

export interface ApiError {
  code: ApiErrorCode;
  status?: number;
  message?: string;
  body?: unknown;
}

export interface ApiErrorBody {
  message?: string;
  error?: string;
  detail?: string;
  title?: string;
}
