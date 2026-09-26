import type { DayMinutes } from '../lib/types';

/** GitHub-style consistency grid: one square per day, darker = more minutes studied. */
export function Heatmap({ days, weeks = 20 }: { days: DayMinutes[]; weeks?: number }) {
  const byDay = new Map(days.map((d) => [d.date, d.minutes]));
  const end = new Date();
  const start = new Date(end);
  start.setDate(end.getDate() - (weeks * 7 - 1) - end.getDay());
  const cells: { key: string; minutes: number; level: number }[] = [];
  for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
    const key = new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
    const m = byDay.get(key) ?? 0;
    cells.push({ key, minutes: m, level: m === 0 ? 0 : m < 30 ? 1 : m < 60 ? 2 : m < 120 ? 3 : 4 });
  }
  return (
    <div>
      <div className="heatmap" role="img" aria-label={`Study minutes per day for the last ${weeks} weeks`}>
        {cells.map((c) => (
          <span key={c.key} data-l={c.level} title={`${c.key}: ${c.minutes} min`} />
        ))}
      </div>
      <div className="row-between" style={{ marginTop: 10 }}>
        <span className="tiny subtle">Last {weeks} weeks</span>
        <span className="heat-legend">
          Less
          {[0, 1, 2, 3, 4].map((l) => (
            <span key={l} style={{ background: `var(--heat-${l})` }} />
          ))}
          More
        </span>
      </div>
    </div>
  );
}
