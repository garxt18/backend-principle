export function formatDate(iso: string | null | undefined, opts: Intl.DateTimeFormatOptions = {}) {
  if (!iso) return '';
  const d = new Date(iso.length === 10 ? `${iso}T00:00:00` : iso);
  return d.toLocaleDateString(undefined, { day: 'numeric', month: 'short', ...opts });
}

export function todayIso() {
  const d = new Date();
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
}

export function hours(minutes: number) {
  const h = minutes / 60;
  return h >= 10 ? `${Math.round(h)}h` : `${Math.round(h * 10) / 10}h`;
}

export function pct(part: number, whole: number) {
  return whole === 0 ? 0 : Math.round((part * 100) / whole);
}

export function youtubeSearch(query: string, hindi: boolean) {
  const q = encodeURIComponent(query.replace(/[&:/]/g, ' ') + (hindi ? ' in hindi' : ' tutorial'));
  return `https://www.youtube.com/results?search_query=${q}`;
}

export function initials(name: string) {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0]!.toUpperCase())
    .join('');
}
