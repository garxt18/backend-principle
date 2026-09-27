import type { AuthResponse } from './types';

/**
 * Fetch wrapper for the Spring Boot API.
 * - The short-lived access token lives only in memory (never localStorage), so an XSS bug cannot steal it
 *   from storage. The long-lived refresh token is an HttpOnly cookie the browser sends to /api/auth/refresh.
 * - On a 401 we refresh once (deduplicated across parallel requests) and retry the original request.
 */
let accessToken: string | null = null;
let refreshing: Promise<AuthResponse | null> | null = null;
let onSessionExpired: () => void = () => {};

export class ApiError extends Error {
  status: number;
  code?: string;
  fieldErrors: Record<string, string>;

  constructor(status: number, body: Record<string, unknown> | null) {
    super((body?.detail as string) || (body?.title as string) || `Request failed (${status})`);
    this.status = status;
    this.code = body?.code as string | undefined;
    this.fieldErrors = (body?.fieldErrors as Record<string, string>) || {};
  }
}

export function setAccessToken(token: string | null) {
  accessToken = token;
}

export function setSessionExpiredHandler(handler: () => void) {
  onSessionExpired = handler;
}

export function refreshSession(): Promise<AuthResponse | null> {
  if (!refreshing) {
    refreshing = fetch('/api/auth/refresh', { method: 'POST', credentials: 'same-origin' })
      .then(async (res) => {
        if (!res.ok) {
          accessToken = null;
          return null;
        }
        const data = (await res.json()) as AuthResponse;
        accessToken = data.accessToken;
        return data;
      })
      .catch(() => null)
      .finally(() => {
        refreshing = null;
      });
  }
  return refreshing;
}

function nonJsonMessage(status: number) {
  if (status === 400 || status === 431) {
    return 'The server rejected this request (headers too large). Clear the cookies for this site, or open it in a private window, then log in again.';
  }
  if (status === 502 || status === 503 || status === 504) return 'The server is starting or unavailable. Try again in a moment.';
  return `Request failed (${status}).`;
}

interface Options {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
  form?: FormData;
  retry?: boolean;
}

export async function api<T>(path: string, { method = 'GET', body, form, retry = true }: Options = {}): Promise<T> {
  const headers: Record<string, string> = {};
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`;
  let payload: BodyInit | undefined;
  if (form) payload = form;
  else if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    payload = JSON.stringify(body);
  }

  const res = await fetch(path, { method, headers, body: payload, credentials: 'same-origin' });

  if (res.status === 401 && retry && !path.startsWith('/api/auth/')) {
    const session = await refreshSession();
    if (session) return api<T>(path, { method, body, form, retry: false });
    onSessionExpired();
    throw new ApiError(401, { detail: 'Your session expired. Please log in again.' });
  }
  if (res.status === 204) return undefined as T;

  const text = await res.text();
  let data: unknown = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      // Not our JSON (e.g. an HTML error page from the web server or a proxy): never show raw HTML to the user.
      data = { detail: nonJsonMessage(res.status) };
    }
  }
  if (!res.ok) throw new ApiError(res.status, data as Record<string, unknown>);
  return data as T;
}
