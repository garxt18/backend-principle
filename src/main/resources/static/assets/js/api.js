// Fetch wrapper. The access token lives only in memory (not localStorage) so XSS cannot read it
// from storage; the refresh token is an HttpOnly cookie the browser sends to /api/auth/refresh.

let accessToken = null;
let refreshing = null;
let onLoggedOut = () => {};

export class ApiError extends Error {
    constructor(status, body) {
        super(body?.detail || body?.title || `Request failed (${status})`);
        this.status = status;
        this.body = body;
        this.fieldErrors = body?.fieldErrors || {};
    }
}

export function setAccessToken(token) { accessToken = token; }
export function onLogout(handler) { onLoggedOut = handler; }

export async function refreshSession() {
    if (!refreshing) {
        refreshing = fetch('/api/auth/refresh', { method: 'POST', credentials: 'same-origin' })
            .then(async res => {
                if (!res.ok) { accessToken = null; return null; }
                const data = await res.json();
                accessToken = data.accessToken;
                return data;
            })
            .finally(() => { refreshing = null; });
    }
    return refreshing;
}

export async function api(path, { method = 'GET', body, form, retry = true } = {}) {
    const headers = {};
    if (accessToken) headers.Authorization = `Bearer ${accessToken}`;
    let payload;
    if (form) payload = form;
    else if (body !== undefined) { headers['Content-Type'] = 'application/json'; payload = JSON.stringify(body); }

    const res = await fetch(path, { method, headers, body: payload, credentials: 'same-origin' });
    if (res.status === 401 && retry && !path.startsWith('/api/auth/')) {
        const session = await refreshSession();
        if (session) return api(path, { method, body, form, retry: false });
        onLoggedOut();
        throw new ApiError(401, { detail: 'Your session expired - please log in again' });
    }
    if (res.status === 204) return null;
    const text = await res.text();
    const data = text ? safeJson(text) : null;
    if (!res.ok) throw new ApiError(res.status, data);
    return data;
}

function safeJson(text) {
    try { return JSON.parse(text); } catch { return { detail: text }; }
}
