// Fetch-based API client
// credentials: "include" keeps session-cookie auth attached.
// All methods throw ApiError on non-2xx responses or network failure

import { API_BASE_URL } from "../config";
import type { ApiError, ApiErrorBody, ApiErrorCode } from "./apiErrors";

function statusToCode(status: number): ApiErrorCode {
  if (status === 400) return "BAD_REQUEST";
  if (status === 401) return "UNAUTHORIZED";
  if (status === 403) return "FORBIDDEN";
  if (status === 404) return "NOT_FOUND";
  if (status === 409) return "CONFLICT";
  return "SERVER_ERROR";
}

function fallbackErrorMessage(response: Response): string {
  if (response.statusText.trim()) {
    return response.statusText;
  }
  return `Request failed with status ${response.status}`;
}

function extractMessageFromBody(body: unknown, fallback: string): string {
  if (typeof body === "string") {
    const trimmed = body.trim();
    return trimmed || fallback;
  }

  if (!body || typeof body !== "object") {
    return fallback;
  }

  const record = body as Record<string, unknown>;
  const messageCandidates = [record.message, record.error, record.detail, record.title];
  for (const candidate of messageCandidates) {
    if (typeof candidate === "string" && candidate.trim()) {
      return candidate.trim();
    }
  }

  if (Array.isArray(record.errors)) {
    const firstMessage = record.errors.find((entry) => typeof entry === "string" && entry.trim());
    if (typeof firstMessage === "string") {
      return firstMessage.trim();
    }
  }

  return fallback;
}

async function readErrorBody(response: Response): Promise<{ message: string; body?: unknown }> {
  const fallback = fallbackErrorMessage(response);
  const raw = await response.text();
  if (!raw.trim()) {
    return { message: fallback };
  }

  const contentType = response.headers.get("content-type") ?? "";
  const looksJson = contentType.includes("application/json") || raw.trim().startsWith("{") || raw.trim().startsWith("[");

  if (looksJson) {
    try {
      const parsed = JSON.parse(raw) as ApiErrorBody | unknown;
      return {
        message: extractMessageFromBody(parsed, fallback),
        body: parsed,
      };
    } catch {
      return { message: raw.trim() || fallback };
    }
  }

  return { message: raw.trim() || fallback };
}

async function request<T>(
  method: string,
  path: string,
  body?: unknown
): Promise<T> {
  const url = `${API_BASE_URL}${path}`;
  const init: RequestInit = {
    method,
    credentials: "include",
    headers: body !== undefined ? { "Content-Type": "application/json" } : {},
  };
  if (body !== undefined) {
    init.body = JSON.stringify(body);
  }

  let response: Response;
  try {
    response = await fetch(url, init);
  } catch {
    const err: ApiError = { code: "NETWORK_ERROR", message: "Network request failed" };
    throw err;
  }

  if (!response.ok) {
    const errorBody = await readErrorBody(response);
    const err: ApiError = {
      code: statusToCode(response.status),
      status: response.status,
      message: errorBody.message,
      body: errorBody.body,
    };
    throw err;
  }

  // 204 No Content - return undefined cast as T
  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

async function formRequest<T>(method: string, path: string, body: FormData): Promise<T> {
  const url = `${API_BASE_URL}${path}`;
  const init: RequestInit = {
    method,
    credentials: "include",
    body,
  };

  let response: Response;
  try {
    response = await fetch(url, init);
  } catch {
    const err: ApiError = { code: "NETWORK_ERROR", message: "Network request failed" };
    throw err;
  }

  if (!response.ok) {
    const errorBody = await readErrorBody(response);
    const err: ApiError = {
      code: statusToCode(response.status),
      status: response.status,
      message: errorBody.message,
      body: errorBody.body,
    };
    throw err;
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

export const apiClient = {
  get<T>(path: string): Promise<T> {
    return request<T>("GET", path);
  },
  post<T>(path: string, body?: unknown): Promise<T> {
    return request<T>("POST", path, body);
  },
  postForm<T>(path: string, body: FormData): Promise<T> {
    return formRequest<T>("POST", path, body);
  },
  put<T>(path: string, body?: unknown): Promise<T> {
    return request<T>("PUT", path, body);
  },
  patch<T>(path: string, body?: unknown): Promise<T> {
    return request<T>("PATCH", path, body);
  },
  delete<T>(path: string): Promise<T> {
    return request<T>("DELETE", path);
  },
};
