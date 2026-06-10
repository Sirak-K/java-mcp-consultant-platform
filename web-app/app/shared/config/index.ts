// Default to same-origin in dev so Vite proxy can eliminate CORS.
// Set VITE_API_BASE_URL explicitly when frontend must call another origin.

export const API_BASE_URL: string =
  (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? "";
