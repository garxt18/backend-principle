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

/**
 * All tabs share one refresh cookie, and each refresh rotates it. The Web Locks API makes tabs take turns,
 * so the second tab sends the NEW cookie instead of racing with the old one (the server also tolerates a
 * short race, for browsers without navigator.locks).
 */
function withRefreshLock<T>(fn: () => Promise<T>): Promise<T> {
  const locks = typeof navigator !== 'undefined' ? navigator.locks : undefined;
  return locks ? (locks.request('pg-refresh', fn) as Promise<T>) : fn();
}

const sleep = (ms: number) => new Promise((resolve) => window.setTimeout(resolve, ms));

/** One refresh call; retried while the server is waking up (502/503/504 or no connection), never on 401. */
async function refreshWithRetry(): Promise<Response | null> {
  for (let attempt = 0; ; attempt++) {
    try {
      const res = await withRefreshLock(() => fetch('/api/auth/refresh', { method: 'POST', credentials: 'same-origin' }));
      if (res.status < 500 || attempt >= 5) return res;
    } catch {
      if (attempt >= 5) return null;
    }
    await sleep(Math.min(1000 * 2 ** attempt, 10_000));
  }
}

export function refreshSession(): Promise<AuthResponse | null> {
  if (!refreshing) {
    refreshing = refreshWithRetry()
      .then(async (res) => {
        if (!res || !res.ok) {
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

/** Authenticated file download (e.g. the Rebuild Lab progress zip): fetch as a blob, then save it via a temporary link. */
export async function downloadFile(path: string, fallbackName: string, retry = true): Promise<void> {
  const headers: Record<string, string> = {};
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`;
  const res = await fetch(path, { headers, credentials: 'same-origin' });
  if (res.status === 401 && retry) {
    if (await refreshSession()) return downloadFile(path, fallbackName, false);
    onSessionExpired();
    throw new ApiError(401, { detail: 'Your session expired. Please log in again.' });
  }
  if (!res.ok) {
    let body: Record<string, unknown> | null = null;
    try {
      body = (await res.json()) as Record<string, unknown>;
    } catch {
      body = { detail: nonJsonMessage(res.status) };
    }
    throw new ApiError(res.status, body);
  }
  const name = /filename="?([^";]+)"?/.exec(res.headers.get('Content-Disposition') ?? '')?.[1] ?? fallbackName;
  const url = URL.createObjectURL(await res.blob());
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}
