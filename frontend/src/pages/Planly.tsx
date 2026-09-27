import { useMutation, useQueryClient } from '@tanstack/react-query';
import { clsx } from 'clsx';
import { CalendarCheck2, RefreshCw, Trash2 } from 'lucide-react';
import { useState, type FormEvent } from 'react';
import { Heatmap } from '../components/Heatmap';
import { Accordion, Button, Check, ConfirmDialog, ErrorState, Field, PageSkeleton, Progress } from '../components/ui';
import { api } from '../lib/api';
import { formatDate, hours, pct, todayIso } from '../lib/format';
import { keys, useHeatmap, usePlan, useRoadmap, useSessions, useSetTopicStatus } from '../lib/queries';
import { useToast } from '../lib/toast';
import type { Level, Plan } from '../lib/types';

export default function Planly() {
  const plan = usePlan();
  const roadmap = useRoadmap();
  const [editing, setEditing] = useState(false);

  if (plan.isPending || roadmap.isPending) return <div className="container"><PageSkeleton /></div>;
  if (plan.error || roadmap.error) return <div className="container page"><ErrorState error={plan.error ?? roadmap.error} /></div>;

  return (
    <div className="container page">
      <div className="page-header">
        <div>
          <div className="eyebrow">Planly</div>
          <h1>Your study plan</h1>
          <p>Your roadmap hours, spread week by week. Tick topics off here or in the Roadmap - both stay in sync.</p>
        </div>
      </div>
      {!plan.data || editing ? (
        <PlanForm levels={roadmap.data!} existing={plan.data ?? null} onDone={() => setEditing(false)} />
      ) : (
        <PlanView plan={plan.data} onReplan={() => setEditing(true)} />
      )}
      <Activity />
    </div>
  );
}

function PlanForm({ levels, existing, onDone }: { levels: Level[]; existing: Plan | null; onDone: () => void }) {
  const qc = useQueryClient();
  const toast = useToast();
  const [start, setStart] = useState(todayIso());
  const [hoursPerWeek, setHours] = useState(existing?.hoursPerWeek ?? 20);
  const [from, setFrom] = useState(0);
  const [to, setTo] = useState(levels.at(-1)?.levelNumber ?? 16);
  const [skip, setSkip] = useState(true);
  const total = levels.filter((l) => l.levelNumber >= from && l.levelNumber <= to).reduce((s, l) => s + l.totalHours, 0);
  const weeks = Math.ceil(total / Math.max(1, hoursPerWeek));

  const create = useMutation({
    mutationFn: () => api<Plan>('/api/plans', { method: 'POST', body: { startDate: start, hoursPerWeek, fromLevel: from, toLevel: to, skipCompleted: skip } }),
    onSuccess: (plan) => {
      qc.setQueryData(keys.plan, plan);
      toast(`Planly created a ${plan.totalWeeks}-week plan`);
      onDone();
    },
    onError: (e) => toast(e.message, 'error'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    create.mutate();
  };

  return (
    <form className="card stack-lg" onSubmit={submit} style={{ animation: 'fade-up var(--dur) var(--ease-out)' }}>
      <div className="row-between">
        <div>
          <h2>{existing ? 'Re-plan' : 'Create your plan'}</h2>
          <p className="small muted" style={{ marginTop: 4 }}>{existing ? 'Your current plan will be archived. Topic progress is kept.' : 'You can re-plan any time - life happens.'}</p>
        </div>
        <CalendarCheck2 size={28} color="var(--accent)" />
      </div>
      <div className="grid grid-3">
        <Field label="Start date"><input className="input" type="date" value={start} onChange={(e) => setStart(e.target.value)} required /></Field>
        <Field label="Study hours per week" hint="20h/week ≈ the roadmap's 6 months"><input className="input" type="number" min={1} max={80} value={hoursPerWeek} onChange={(e) => setHours(Number(e.target.value))} required /></Field>
        <Field label="From level">
          <select className="select" value={from} onChange={(e) => setFrom(Number(e.target.value))}>
            {levels.map((l) => <option key={l.id} value={l.levelNumber}>L{l.levelNumber} · {l.title}</option>)}
          </select>
        </Field>
        <Field label="To level">
          <select className="select" value={to} onChange={(e) => setTo(Number(e.target.value))}>
            {levels.map((l) => <option key={l.id} value={l.levelNumber}>L{l.levelNumber} · {l.title}</option>)}
          </select>
        </Field>
        <label className="row small" style={{ alignSelf: 'end', height: 38, gap: 10, cursor: 'pointer' }}>
          <Check checked={skip} onChange={setSkip} label="Skip topics I already finished" /> Skip topics I already finished
        </label>
      </div>
      <div className="alert alert-info">
        About <strong>&nbsp;{total}h&nbsp;</strong> of roadmap work → roughly <strong>&nbsp;{weeks} weeks&nbsp;</strong> ({Math.max(1, Math.round(weeks / 4.3))} months) at {hoursPerWeek}h/week. DSA practice runs alongside on Striver's A2Z sheet (linked on your dashboard).
      </div>
      <div className="row">
        <Button type="submit" variant="primary" loading={create.isPending}>Generate plan</Button>
        {existing && <Button variant="ghost" onClick={onDone}>Cancel</Button>}
      </div>
    </form>
  );
}

function PlanView({ plan, onReplan }: { plan: Plan; onReplan: () => void }) {
  const qc = useQueryClient();
  const toast = useToast();
  const setStatus = useSetTopicStatus();
  const [open, setOpen] = useState<Set<number>>(new Set([plan.currentWeek]));
  const [confirm, setConfirm] = useState(false);
  const archive = useMutation({
    mutationFn: () => api('/api/plans/current', { method: 'DELETE' }),
    onSuccess: () => {
      qc.setQueryData(keys.plan, null);
      toast('Plan archived');
    },
  });
  const delta = plan.scheduleDeltaHours;
  const toggle = (w: number) => setOpen((s) => { const n = new Set(s); if (n.has(w)) n.delete(w); else n.add(w); return n; });

  return (
    <>
      <div className="card" style={{ marginBottom: 16 }}>
        <div className="row-between">
          <div>
            <h2>{plan.totalWeeks}-week plan</h2>
            <div className="small muted" style={{ marginTop: 4 }}>
              {formatDate(plan.startDate, { year: 'numeric' })} → {formatDate(plan.endDate, { year: 'numeric' })} · {plan.hoursPerWeek}h/week · week {plan.currentWeek}
            </div>
          </div>
          <div className="row">
            <span className={clsx('badge', delta < 0 ? 'badge-danger' : 'badge-success')}>{delta > 0 ? `${delta}h ahead` : delta < 0 ? `${-delta}h behind` : 'On track'}</span>
            <Button size="sm" variant="outline" onClick={onReplan}><RefreshCw size={14} /> Re-plan</Button>
            <Button size="sm" variant="danger" onClick={() => setConfirm(true)}><Trash2 size={14} /> Archive</Button>
          </div>
        </div>
        <div style={{ marginTop: 14 }}>
          <div className="row-between small"><span className="muted">Planned hours completed</span><strong>{plan.percentDone}%</strong></div>
          <Progress value={plan.percentDone} />
        </div>
      </div>

      {plan.weeks.map((w) => {
        const done = w.items.filter((i) => i.done).length;
        const state = w.week < plan.currentWeek ? 'past' : w.week === plan.currentWeek ? 'current' : '';
        return (
          <Accordion
            key={w.week}
            open={open.has(w.week)}
            onToggle={() => toggle(w.week)}
            leading={<span className={clsx('timeline-dot', state)} />}
            title={<>Week {w.week}{w.week === plan.currentWeek && <span className="badge badge-accent" style={{ marginLeft: 8 }}>This week</span>}</>}
            subtitle={`${formatDate(w.startDate)} – ${formatDate(w.endDate)} · ${w.plannedHours}h planned · ${hours(w.minutesLogged)} logged`}
            meta={<><Progress value={pct(done, w.items.length)} /><span className="acc-count">{done} / {w.items.length}</span></>}
          >
            <div style={{ marginTop: 6 }}>
              {w.items.map((item) => (
                <div key={`${item.topicId}-${item.plannedHours}`} className={clsx('plan-item', item.done && 'done')}>
                  <Check checked={item.done} label={`Mark ${item.topicTitle} done`} onChange={(v) => setStatus.mutate({ topicId: item.topicId, status: v ? 'DONE' : 'IN_PROGRESS' })} />
                  <span className="pi-title grow">{item.topicTitle}</span>
                  <span className="badge hide-sm">L{item.levelNumber} · {item.levelTitle.split('(')[0]!.trim()}</span>
                  <span className="small subtle nowrap">{item.plannedHours}h</span>
                </div>
              ))}
            </div>
          </Accordion>
        );
      })}
      {confirm && (
        <ConfirmDialog
          title="Archive this plan?"
          body="Your topic progress and study sessions are kept. You can create a new plan right after."
          confirmLabel="Archive plan"
          danger
          onClose={() => setConfirm(false)}
          onConfirm={() => { setConfirm(false); archive.mutate(); }}
        />
      )}
    </>
  );
}

function Activity() {
  const sessions = useSessions();
  const heat = useHeatmap();
  const roadmap = useRoadmap();
  const qc = useQueryClient();
  const toast = useToast();
  const titles = new Map((roadmap.data ?? []).flatMap((l) => l.topics.map((t) => [t.id, t.title] as const)));
  const remove = useMutation({
    mutationFn: (id: number) => api(`/api/sessions/${id}`, { method: 'DELETE' }),
    onSuccess: () => {
      [keys.sessions, keys.heatmap, keys.progress, keys.plan].forEach((k) => qc.invalidateQueries({ queryKey: k }));
      toast('Session deleted');
    },
  });
  return (
    <div className="grid grid-2" style={{ marginTop: 16 }}>
      <div className="card">
        <div className="card-head"><span className="card-title">Consistency</span></div>
        {heat.data && <Heatmap days={heat.data} weeks={18} />}
      </div>
      <div className="card">
        <div className="card-head"><span className="card-title">Recent sessions</span><span className="small subtle">log time from the dashboard</span></div>
        {!sessions.data?.length ? (
          <p className="muted small">No sessions yet.</p>
        ) : (
          sessions.data.slice(0, 8).map((s) => (
            <div key={s.id} className="plan-item">
              <span className="small subtle nowrap" style={{ width: 54 }}>{formatDate(s.date)}</span>
              <span className="grow truncate small">{s.topicId ? titles.get(s.topicId) : s.note || 'Study session'}</span>
              <span className="small nowrap">{s.minutes}m</span>
              <Button size="sm" variant="ghost" icon aria-label="Delete session" onClick={() => remove.mutate(s.id)}><Trash2 size={14} /></Button>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
