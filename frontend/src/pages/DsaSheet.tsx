import { clsx } from 'clsx';
import { ExternalLink, NotebookPen, Search } from 'lucide-react';
import { Youtube } from '../components/icons';
import { useDeferredValue, useMemo, useState } from 'react';
import { Accordion, Button, Check, Chips, Difficulty, Empty, ErrorState, LangBadge, PageSkeleton, Progress, Ring, Star } from '../components/ui';
import { pct } from '../lib/format';
import { useDsaSheet, useUpdateProblem } from '../lib/queries';
import { useToast } from '../lib/toast';
import type { DsaProblem } from '../lib/types';

type DiffFilter = 'ALL' | 'EASY' | 'MEDIUM' | 'HARD';
type StatusFilter = 'ALL' | 'TODO' | 'SOLVED' | 'REVISION';

function ytQuery(title: string, hindi: boolean) {
  const q = encodeURIComponent(`${title} leetcode ${hindi ? 'hindi' : 'solution'}`);
  return `https://www.youtube.com/results?search_query=${q}`;
}

export default function DsaSheet() {
  const sheet = useDsaSheet();
  const [open, setOpen] = useState<Set<number>>(new Set());
  const [diff, setDiff] = useState<DiffFilter>('ALL');
  const [status, setStatus] = useState<StatusFilter>('ALL');
  const [query, setQuery] = useState('');
  const deferredQuery = useDeferredValue(query.trim().toLowerCase());

  const filtering = diff !== 'ALL' || status !== 'ALL' || deferredQuery !== '';
  const topics = useMemo(() => {
    if (!sheet.data) return [];
    return sheet.data.topics
      .map((t) => ({
        ...t,
        visible: t.problems.filter((p) =>
          (diff === 'ALL' || p.difficulty === diff) &&
          (status === 'ALL' || (status === 'TODO' && !p.solved) || (status === 'SOLVED' && p.solved) || (status === 'REVISION' && p.revision)) &&
          (!deferredQuery || p.title.toLowerCase().includes(deferredQuery))),
      }))
      .filter((t) => !filtering || t.visible.length > 0);
  }, [sheet.data, diff, status, deferredQuery, filtering]);

  if (sheet.isPending) return <div className="container"><PageSkeleton /></div>;
  if (sheet.error) return <div className="container page"><ErrorState error={sheet.error} /></div>;
  const s = sheet.data!.stats;
  const toggle = (id: number) => setOpen((o) => { const n = new Set(o); if (n.has(id)) n.delete(id); else n.add(id); return n; });

  return (
    <div className="container page">
      <div className="page-header">
        <div>
          <div className="eyebrow">Interview preparation</div>
          <h1>DSA Sheet</h1>
          <p>{s.total} hand-picked problems in 16 patterns, easy to hard inside each step. Solve on LeetCode, tick it here, star it for revision.</p>
        </div>
      </div>

      <div className="grid grid-3" style={{ marginBottom: 16 }}>
        <div className="card row" style={{ gap: 18 }}>
          <Ring value={pct(s.solved, s.total)} size={84} stroke={7}><div><strong>{s.solved}</strong><div className="tiny subtle">/ {s.total}</div></div></Ring>
          <div className="stack" style={{ gap: 4 }}>
            <span className="card-title">Total progress</span>
            <span className="small muted">{s.solvedLast7Days} solved this week</span>
            <span className="small muted">{s.revision} starred for revision</span>
          </div>
        </div>
        <div className="card diff-bar" style={{ gridColumn: 'span 2' }}>
          {([['Easy', s.easy, 'var(--easy)'], ['Medium', s.medium, 'var(--medium)'], ['Hard', s.hard, 'var(--hard)']] as const).map(([label, c, color]) => (
            <div key={label}>
              <div className="row-between small"><span style={{ color, fontWeight: 600 }}>{label}</span><span className="muted">{c.solved} / {c.total}</span></div>
              <div className="progress" style={{ marginTop: 6 }}><span style={{ width: `${pct(c.solved, c.total)}%`, background: color }} /></div>
            </div>
          ))}
        </div>
      </div>

      <div className="row-between" style={{ marginBottom: 14 }}>
        <div className="row">
          <Chips<DiffFilter> label="Difficulty" value={diff} onChange={setDiff} options={[{ value: 'ALL', label: 'All' }, { value: 'EASY', label: 'Easy' }, { value: 'MEDIUM', label: 'Medium' }, { value: 'HARD', label: 'Hard' }]} />
          <Chips<StatusFilter> label="Status" value={status} onChange={setStatus} options={[{ value: 'ALL', label: 'Any status' }, { value: 'TODO', label: 'To do' }, { value: 'SOLVED', label: 'Solved' }, { value: 'REVISION', label: 'Revision' }]} />
        </div>
        <div className="input-with-icon" style={{ width: 'min(260px, 100%)' }}>
          <Search size={15} />
          <input className="input" placeholder="Search problems" value={query} onChange={(e) => setQuery(e.target.value)} aria-label="Search problems" />
        </div>
      </div>

      {topics.length === 0 && <Empty icon={<Search size={32} />} title="No problems match these filters" />}

      {topics.map((t, i) => (
        <Accordion
          key={t.id}
          open={filtering || open.has(t.id)}
          onToggle={() => toggle(t.id)}
          leading={<span className="step-num">{i + 1}</span>}
          title={t.title}
          subtitle={t.summary ?? undefined}
          meta={<><Progress value={pct(t.solved, t.total)} tone={t.solved === t.total ? 'success' : undefined} /><span className="acc-count">{t.solved} / {t.total}</span></>}
        >
          <div className="row" style={{ margin: '12px 0 4px', gap: 6 }}>
            {t.resources.map((r) => (
              <a key={r.url} href={r.url} target="_blank" rel="noopener noreferrer" className="btn btn-sm btn-outline">
                <Youtube size={14} color="#ff3b3b" /> {r.channel ?? 'Search'} <LangBadge lang={r.language} />
              </a>
            ))}
          </div>
          <div className="table-scroll">
            <table className="sheet">
              <thead>
                <tr>
                  <th className="col-check">Status</th>
                  <th>Problem</th>
                  <th className="col-icons">Resources</th>
                  <th className="col-diff hide-sm">Difficulty</th>
                  <th className="col-star">Revise</th>
                </tr>
              </thead>
              <tbody>
                {t.visible.map((p) => <ProblemRow key={p.id} p={p} />)}
              </tbody>
            </table>
          </div>
        </Accordion>
      ))}
    </div>
  );
}

function ProblemRow({ p }: { p: DsaProblem }) {
  const update = useUpdateProblem();
  const toast = useToast();
  const [notesOpen, setNotesOpen] = useState(false);
  const [notes, setNotes] = useState(p.notes ?? '');
  const onError = (e: Error) => toast(e.message, 'error');
  return (
    <>
      <tr className={clsx(p.solved && 'is-done')}>
        <td><Check checked={p.solved} label={`Mark ${p.title} solved`} onChange={(v) => update.mutate({ id: p.id, solved: v }, { onError, onSuccess: () => v && toast(`${p.title} solved 🎯`) })} /></td>
        <td>
          <a href={p.url} target="_blank" rel="noopener noreferrer" className="p-title" style={{ fontWeight: 550 }}>{p.title}</a>
        </td>
        <td>
          <span className="row" style={{ gap: 2, flexWrap: 'nowrap' }}>
            <a className="icon-link lc" href={p.url} target="_blank" rel="noopener noreferrer" title="Solve on LeetCode" aria-label={`Solve ${p.title} on LeetCode`}><ExternalLink size={16} /></a>
            <a className="icon-link yt" href={ytQuery(p.title, true)} target="_blank" rel="noopener noreferrer" title="Video explanation in Hindi" aria-label="Hindi video"><Youtube size={17} /></a>
            <a className="icon-link yt" href={ytQuery(p.title, false)} target="_blank" rel="noopener noreferrer" title="Video explanation in English" aria-label="English video" style={{ fontSize: 10, fontWeight: 700 }}>EN</a>
            <button className="icon-link" style={{ border: 0, background: 'none', color: p.notes ? 'var(--accent)' : undefined }} title="Notes" aria-label="Notes" onClick={() => setNotesOpen((o) => !o)}><NotebookPen size={16} /></button>
          </span>
        </td>
        <td className="hide-sm"><Difficulty value={p.difficulty} /></td>
        <td className="col-star"><Star on={p.revision} label={p.revision ? 'Remove from revision' : 'Mark for revision'} onChange={(v) => update.mutate({ id: p.id, revision: v }, { onError })} /></td>
      </tr>
      {notesOpen && (
        <tr>
          <td />
          <td colSpan={4}>
            <div className="stack" style={{ animation: 'fade-up var(--dur) var(--ease-out)' }}>
              <textarea className="textarea" rows={3} value={notes} maxLength={10000} onChange={(e) => setNotes(e.target.value)} placeholder="Approach, time/space complexity, edge cases..." />
              <div><Button size="sm" variant="primary" loading={update.isPending} onClick={() => update.mutate({ id: p.id, notes }, { onError, onSuccess: () => toast('Notes saved') })}>Save notes</Button></div>
            </div>
          </td>
        </tr>
      )}
    </>
  );
}
