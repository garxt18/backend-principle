import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { clsx } from 'clsx';
import {
  AlertTriangle, CalendarCheck2, CalendarDays, ChevronLeft, ChevronRight, Clock, ExternalLink, Flag, ListChecks, Play, Plus, RefreshCw, Rocket, Sun, Timer, Trash2,
} from 'lucide-react';
import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { Heatmap } from '../components/Heatmap';
import { Accordion, Button, Check, Chips, ConfirmDialog, ErrorState, Field, PageSkeleton, Progress } from '../components/ui';
import { api } from '../lib/api';
import { addDaysIso, formatDate, hours, hoursLabel, pct, todayIso, weekday } from '../lib/format';
import { watchFor } from '../lib/learn';
import { keys, useHeatmap, useMyResources, usePlan, useProgress, useReplan, useRoadmap, useSessions, useSetTopicStatus } from '../lib/queries';
import { useToast } from '../lib/toast';
import type { Level, PaceMode, Plan, PlanDay, PlanItem, PlanPreview, PlanRequest, Weekday } from '../lib/types';

const WEEKDAYS: { value: Weekday; short: string }[] = [
  { value: 'MONDAY', short: 'M' }, { value: 'TUESDAY', short: 'T' }, { value: 'WEDNESDAY', short: 'W' }, { value: 'THURSDAY', short: 'T' },
  { value: 'FRIDAY', short: 'F' }, { value: 'SATURDAY', short: 'S' }, { value: 'SUNDAY', short: 'S' },
];
const ALL_DAYS = WEEKDAYS.map((d) => d.value);
const MON_SAT = ALL_DAYS.slice(0, 6);

/** One-click starting points: "learn X in Y". Levels are matched by slug so renumbering cannot break them. */
const PRESETS: { label: string; hint: string; slugs: string[]; days: number }[] = [
  { label: 'Java Basics in 7 days', hint: 'Lectures 1-21, a focused sprint', slugs: ['programming-fundamentals'], days: 7 },
  { label: 'Advanced Java in 2 weeks', hint: 'Lectures 22-57', slugs: ['core-java'], days: 14 },
  { label: 'Spring Boot in 30 days', hint: 'Spring lectures 1-40', slugs: ['spring-boot'], days: 30 },
  { label: 'Java + Spring in 2 months', hint: 'Levels 0-3', slugs: ['programming-fundamentals', 'git-developer-tools', 'core-java', 'spring-boot'], days: 60 },
  { label: 'Whole roadmap in 6 months', hint: 'All 17 levels', slugs: [], days: 182 },
];
const DEADLINES = [{ label: '3 days', days: 3 }, { label: '1 week', days: 7 }, { label: '2 weeks', days: 14 }, { label: '1 month', days: 30 }, { label: '3 months', days: 91 }, { label: '6 months', days: 182 }];

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
          <p>Pick what to learn and by when - a few days, a week, a month. Planly turns the roadmap into a day-by-day schedule, and ticking a topic here or in the Roadmap is the same tick.</p>
        </div>
        {plan.data && !editing && <Button variant="outline" onClick={() => setEditing(true)}><Plus size={15} /> New plan</Button>}
      </div>
      {!plan.data || editing ? (
        <PlanBuilder levels={roadmap.data!} hasPlan={!!plan.data} onDone={() => setEditing(false)} />
      ) : (
        <PlanView plan={plan.data} levels={roadmap.data!} />
      )}
      <Activity />
    </div>
  );
}

// =====================================================================================================
// Builder
// =====================================================================================================

function PlanBuilder({ levels, hasPlan, onDone }: { levels: Level[]; hasPlan: boolean; onDone: () => void }) {
  const qc = useQueryClient();
  const toast = useToast();
  const progress = useProgress();
  const doneIds = useMemo(() => new Set((progress.data?.topics ?? []).filter((t) => t.status === 'DONE').map((t) => t.topicId)), [progress.data]);

  const [name, setName] = useState('');
  const [selected, setSelected] = useState<number[]>(() => levels.slice(0, 1).map((l) => l.levelNumber));
  const [customTopics, setCustomTopics] = useState<Set<number> | null>(null);
  const [pace, setPace] = useState<PaceMode>('DEADLINE');
  const [start, setStart] = useState(todayIso());
  const [deadline, setDeadline] = useState(addDaysIso(todayIso(), 6));
  const [hoursPerDay, setHoursPerDay] = useState(2);
  const [days, setDays] = useState<Weekday[]>(MON_SAT);
  const [skip, setSkip] = useState(true);

  const applyPreset = (p: (typeof PRESETS)[number]) => {
    const nums = p.slugs.length ? levels.filter((l) => p.slugs.includes(l.slug)).map((l) => l.levelNumber) : levels.map((l) => l.levelNumber);
    setSelected(nums);
    setCustomTopics(null);
    setPace('DEADLINE');
    setDeadline(addDaysIso(start, p.days - 1));
    setName(p.label);
  };
  const toggleLevel = (n: number) => {
    setCustomTopics(null);
    setSelected((s) => (s.includes(n) ? s.filter((x) => x !== n) : [...s, n].sort((a, b) => a - b)));
  };
  const toggleDay = (d: Weekday) => setDays((s) => (s.includes(d) ? s.filter((x) => x !== d) : ALL_DAYS.filter((x) => x === d || s.includes(x))));

  const request: PlanRequest = {
    name: name.trim() || undefined,
    startDate: start,
    pace,
    ...(pace === 'DEADLINE' ? { targetEndDate: deadline } : { hoursPerDay }),
    studyDays: days,
    levelNumbers: selected,
    ...(customTopics ? { topicIds: [...customTopics] } : {}),
    skipCompleted: skip,
  };
  const valid = selected.length > 0 && days.length > 0 && (!customTopics || customTopics.size > 0) && (pace === 'HOURS' || deadline >= start);

  // Live preview, debounced so typing a date does not fire a request per keystroke.
  const [debounced, setDebounced] = useState(request);
  const requestKey = JSON.stringify(request);
  useEffect(() => {
    const t = window.setTimeout(() => setDebounced(JSON.parse(requestKey) as PlanRequest), 250);
    return () => window.clearTimeout(t);
  }, [requestKey]);
  const preview = useQuery({
    queryKey: ['plan-preview', debounced],
    queryFn: () => api<PlanPreview>('/api/plans/preview', { method: 'POST', body: debounced }),
    enabled: valid,
    retry: false,
    placeholderData: (prev) => prev,
  });

  const create = useMutation({
    mutationFn: () => api<Plan>('/api/plans', { method: 'POST', body: request }),
    onSuccess: (plan) => {
      qc.setQueryData(keys.plan, plan);
      toast(`Planly created your plan: ${plan.topicsTotal} topics, done by ${formatDate(plan.endDate)}`);
      onDone();
    },
    onError: (e) => toast(e.message, 'error'),
  });
  const submit = (e: FormEvent) => {
    e.preventDefault();
    create.mutate();
  };

  const selectedLevels = levels.filter((l) => selected.includes(l.levelNumber));
  const tooMuch = (preview.data?.hoursPerDay ?? 0) > 16;

  return (
    <form className="stack-lg" onSubmit={submit} style={{ animation: 'fade-up var(--dur) var(--ease-out)' }}>
      <section className="card">
        <div className="builder-step"><span className="step-num">1</span><div><h2>What do you want to learn?</h2><p className="small muted">Start from a preset or pick levels yourself.</p></div></div>
        <div className="preset-grid">
          {PRESETS.map((p) => (
            <button key={p.label} type="button" className={clsx('preset', name === p.label && 'on')} onClick={() => applyPreset(p)}>
              <Rocket size={15} />
              <span><span className="preset-title">{p.label}</span><span className="preset-hint">{p.hint}</span></span>
            </button>
          ))}
        </div>
        <div className="level-picker">
          {levels.map((l) => (
            <button key={l.id} type="button" className={clsx('level-chip', selected.includes(l.levelNumber) && 'on')} onClick={() => toggleLevel(l.levelNumber)} aria-pressed={selected.includes(l.levelNumber)}>
              <span className="lc-num">{l.levelNumber}</span>
              <span className="lc-title">{l.title}</span>
              <span className="lc-hours">{l.totalHours}h</span>
            </button>
          ))}
        </div>
        {selectedLevels.length > 0 && (
          <div style={{ marginTop: 12 }}>
            <Button type="button" size="sm" variant="ghost" onClick={() => setCustomTopics(customTopics ? null : new Set(selectedLevels.flatMap((l) => l.topics.map((t) => t.id))))}>
              <ListChecks size={14} /> {customTopics ? 'Use whole levels again' : 'Pick specific topics / lectures'}
            </Button>
            {customTopics && (
              <div className="topic-picker">
                {selectedLevels.map((l) => (
                  <div key={l.id}>
                    <div className="row-between tiny" style={{ margin: '8px 0 4px' }}>
                      <strong>L{l.levelNumber} · {l.title}</strong>
                      <span className="row" style={{ gap: 8 }}>
                        <button type="button" className="text-btn" onClick={() => setCustomTopics((s) => new Set([...(s ?? []), ...l.topics.map((t) => t.id)]))}>all</button>
                        <button type="button" className="text-btn" onClick={() => setCustomTopics((s) => new Set([...(s ?? [])].filter((id) => !l.topics.some((t) => t.id === id))))}>none</button>
                      </span>
                    </div>
                    {l.topics.map((t) => (
                      <label key={t.id} className={clsx('topic-pick', doneIds.has(t.id) && 'done')}>
                        <input type="checkbox" checked={customTopics.has(t.id)} onChange={(e) => setCustomTopics((s) => { const n = new Set(s); if (e.target.checked) n.add(t.id); else n.delete(t.id); return n; })} />
                        {t.lectureNumber && <span className="lecture-no">L{t.lectureNumber}</span>}
                        <span className="grow">{t.title}</span>
                        <span className="subtle nowrap">{t.estimatedHours}h</span>
                      </label>
                    ))}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </section>

      <section className="card">
        <div className="builder-step"><span className="step-num">2</span><div><h2>How fast?</h2><p className="small muted">Set a finish date and Planly works out the daily time - or tell it your daily time and it works out the date.</p></div></div>
        <Chips<PaceMode> label="Plan by" value={pace} onChange={setPace} options={[{ value: 'DEADLINE', label: <span className="row" style={{ gap: 6 }}><Flag size={14} /> Finish by a date</span> }, { value: 'HOURS', label: <span className="row" style={{ gap: 6 }}><Timer size={14} /> Hours per day</span> }]} />
        <div className="grid grid-3" style={{ marginTop: 14 }}>
          <Field label="Start"><input className="input" type="date" value={start} onChange={(e) => setStart(e.target.value)} required /></Field>
          {pace === 'DEADLINE' ? (
            <Field label="Finish by"><input className="input" type="date" value={deadline} min={start} onChange={(e) => setDeadline(e.target.value)} required /></Field>
          ) : (
            <Field label="Hours per study day" hint={`${hoursLabel(hoursPerDay)} a day`}><input className="input" type="number" min={0.5} max={16} step={0.25} value={hoursPerDay} onChange={(e) => setHoursPerDay(Number(e.target.value))} required /></Field>
          )}
          <Field label="Name (optional)"><input className="input" maxLength={120} value={name} placeholder="e.g. Diwali Java sprint" onChange={(e) => setName(e.target.value)} /></Field>
        </div>
        {pace === 'DEADLINE' && (
          <div className="row" style={{ marginTop: 10 }}>
            {DEADLINES.map((d) => (
              <button key={d.label} type="button" className={clsx('pill', deadline === addDaysIso(start, d.days - 1) && 'on')} onClick={() => setDeadline(addDaysIso(start, d.days - 1))}>{d.label}</button>
            ))}
          </div>
        )}
        <div style={{ marginTop: 16 }}>
          <span className="field-label">Study days</span>
          <div className="day-toggles" role="group" aria-label="Study days">
            {WEEKDAYS.map((d) => (
              <button key={d.value} type="button" className={clsx('day-toggle', days.includes(d.value) && 'on')} onClick={() => toggleDay(d.value)} aria-pressed={days.includes(d.value)} title={d.value.toLowerCase()}>{d.short}</button>
            ))}
          </div>
        </div>
        <label className="row small" style={{ marginTop: 14, gap: 10, cursor: 'pointer' }}>
          <Check checked={skip} onChange={setSkip} label="Skip topics I already finished" /> Skip topics I already finished
        </label>
      </section>

      <section className={clsx('card preview-card', tooMuch && 'bad')}>
        {!valid ? (
          <p className="muted">Choose at least one level (or topic) and one study day.</p>
        ) : preview.error ? (
          <p className="alert alert-danger">{(preview.error as Error).message}</p>
        ) : !preview.data ? (
          <div className="skeleton" style={{ height: 60 }} />
        ) : (
          <>
            <div className="preview-stats">
              <div><span className="ps-value">{preview.data.topics}</span><span className="ps-label">topics</span></div>
              <div><span className="ps-value">{hoursLabel(preview.data.totalHours)}</span><span className="ps-label">of study</span></div>
              <div><span className="ps-value">{hoursLabel(preview.data.hoursPerDay)}</span><span className="ps-label">per study day</span></div>
              <div><span className="ps-value">{preview.data.studyDays}</span><span className="ps-label">study days</span></div>
              <div><span className="ps-value">{formatDate(preview.data.endDate)}</span><span className="ps-label">finish ({weekday(preview.data.endDate)})</span></div>
            </div>
            {preview.data.warning && <p className={clsx('alert', tooMuch ? 'alert-danger' : 'alert-info')} style={{ marginTop: 12 }}><AlertTriangle size={15} /> {preview.data.warning}</p>}
          </>
        )}
        <div className="row" style={{ marginTop: 14 }}>
          <Button type="submit" variant="primary" loading={create.isPending} disabled={!valid || tooMuch || !!preview.error}><CalendarCheck2 size={15} /> {hasPlan ? 'Replace my plan' : 'Create my plan'}</Button>
          {hasPlan && <Button type="button" variant="ghost" onClick={onDone}>Cancel</Button>}
          {hasPlan && <span className="tiny subtle">Your current plan is archived; topic progress is kept.</span>}
        </div>
      </section>
    </form>
  );
}

// =====================================================================================================
// Plan view
// =====================================================================================================

type Tab = 'today' | 'week' | 'all';

function PlanView({ plan, levels }: { plan: Plan; levels: Level[] }) {
  const qc = useQueryClient();
  const toast = useToast();
  const replan = useReplan();
  const [tab, setTab] = useState<Tab>('today');
  const [confirm, setConfirm] = useState(false);
  const archive = useMutation({
    mutationFn: () => api('/api/plans/current', { method: 'DELETE' }),
    onSuccess: () => {
      qc.setQueryData(keys.plan, null);
      toast('Plan archived');
    },
  });
  const doReplan = (keepDeadline: boolean) => replan.mutate(keepDeadline, {
    onSuccess: (p) => toast(`Re-planned: ${p.topicsTotal - p.topicsDone} topics left, done by ${formatDate(p.endDate)}`),
    onError: (e) => toast(e.message, 'error'),
  });
  const delta = plan.scheduleDeltaHours;
  const late = plan.targetEndDate && plan.projectedEndDate && plan.projectedEndDate > plan.targetEndDate;

  return (
    <>
      <div className="card plan-summary">
        <div className="row-between" style={{ alignItems: 'flex-start' }}>
          <div style={{ minWidth: 0 }}>
            <div className="row" style={{ gap: 6, marginBottom: 6 }}>
              <span className="badge badge-accent">{plan.paceMode === 'DEADLINE' ? 'Deadline plan' : 'Pace plan'}</span>
              <span className={clsx('badge', delta < 0 ? 'badge-danger' : 'badge-success')}>{delta > 0 ? `${delta}h ahead` : delta < 0 ? `${-delta}h behind` : 'On track'}</span>
            </div>
            <h2>{plan.name ?? `${plan.topicsTotal}-topic plan`}</h2>
            <div className="small muted" style={{ marginTop: 4 }}>
              {formatDate(plan.startDate)} → {formatDate(plan.endDate, { year: 'numeric' })} · {hoursLabel(plan.hoursPerDay)}/day on {plan.studyDays.length === 7 ? 'every day' : plan.studyDays.map((d) => d.slice(0, 3).toLowerCase()).join(', ')}
            </div>
          </div>
          <div className="row">
            <Button size="sm" variant="outline" loading={replan.isPending} onClick={() => doReplan(false)} title="Move everything not done yet to start today, at the same daily time"><RefreshCw size={14} /> Re-plan rest</Button>
            {plan.targetEndDate && <Button size="sm" variant="ghost" loading={replan.isPending} onClick={() => doReplan(true)} title="Keep the finish date; the daily time goes up">Keep deadline</Button>}
            <Button size="sm" variant="danger" onClick={() => setConfirm(true)}><Trash2 size={14} /> Archive</Button>
          </div>
        </div>
        <div className="plan-meters">
          <div>
            <div className="row-between small"><span className="muted">Topics</span><strong>{plan.topicsDone} / {plan.topicsTotal}</strong></div>
            <Progress value={pct(plan.topicsDone, plan.topicsTotal)} />
          </div>
          <div>
            <div className="row-between small"><span className="muted">Planned hours done</span><strong>{plan.percentDone}%</strong></div>
            <Progress value={plan.percentDone} tone="success" />
          </div>
        </div>
        {plan.projectedEndDate && plan.projectedEndDate !== plan.endDate && (
          <p className={clsx('small', late ? 'text-danger' : 'muted')} style={{ marginTop: 10 }}>
            At {hoursLabel(plan.hoursPerDay)} a day from today you finish on <strong>{formatDate(plan.projectedEndDate, { year: 'numeric' })}</strong>
            {late ? ` - after your ${formatDate(plan.targetEndDate!)} deadline. "Keep deadline" raises the daily time.` : '.'}
          </p>
        )}
      </div>

      <div className="row-between" style={{ margin: '18px 0 12px' }}>
        <Chips<Tab> label="View" value={tab} onChange={setTab} options={[
          { value: 'today', label: <span className="row" style={{ gap: 6 }}><Sun size={14} /> Today</span> },
          { value: 'week', label: <span className="row" style={{ gap: 6 }}><CalendarDays size={14} /> Week</span> },
          { value: 'all', label: <span className="row" style={{ gap: 6 }}><ListChecks size={14} /> Full schedule</span> },
        ]} />
        <Link to="/roadmap" className="link small">Open the roadmap</Link>
      </div>

      {tab === 'today' && <TodayView plan={plan} levels={levels} onReplan={() => doReplan(false)} replanning={replan.isPending} />}
      {tab === 'week' && <WeekView plan={plan} levels={levels} />}
      {tab === 'all' && <FullSchedule plan={plan} levels={levels} />}

      {confirm && (
        <ConfirmDialog
          title="Archive this plan?"
          body="Your topic progress and study sessions are kept. You can create a new plan right after."
          confirmLabel="Archive"
          danger
          onConfirm={() => { setConfirm(false); archive.mutate(); }}
          onClose={() => setConfirm(false)}
        />
      )}
    </>
  );
}

function useTopicIndex(levels: Level[]) {
  return useMemo(() => {
    const map = new Map<number, { level: Level; topic: Level['topics'][number] }>();
    levels.forEach((level) => level.topics.forEach((topic) => map.set(topic.id, { level, topic })));
    return map;
  }, [levels]);
}

/** One planned topic: tick it (same tick as the roadmap), watch the lecture, log the planned time. */
function ItemRow({ item, levels, showDate }: { item: PlanItem; levels: Level[]; showDate?: boolean }) {
  const setStatus = useSetTopicStatus();
  const mine = useMyResources();
  const index = useTopicIndex(levels);
  const qc = useQueryClient();
  const toast = useToast();
  const entry = index.get(item.topicId);
  const watch = entry ? watchFor(entry.level, entry.topic, mine.data) : null;
  const log = useMutation({
    mutationFn: () => api('/api/sessions', { method: 'POST', body: { minutes: Math.max(1, Math.round(item.plannedHours * 60)), date: todayIso(), topicId: item.topicId } }),
    onSuccess: () => {
      toast(`Logged ${hoursLabel(item.plannedHours)} on ${item.topicTitle}`);
      [keys.progress, keys.heatmap, keys.sessions, keys.plan].forEach((k) => qc.invalidateQueries({ queryKey: k }));
    },
    onError: (e) => toast(e.message, 'error'),
  });
  return (
    <div className={clsx('plan-item', 'plan-item-rich', item.done && 'done')}>
      <Check checked={item.done} label={`Mark ${item.topicTitle} done`} onChange={(v) => setStatus.mutate({ topicId: item.topicId, status: v ? 'DONE' : 'IN_PROGRESS' })} />
      <div className="grow" style={{ minWidth: 0 }}>
        <div className="pi-title">
          {item.lectureNumber && <span className="lecture-no">L{item.lectureNumber}</span>}
          {item.topicTitle}
        </div>
        <div className="tiny subtle">
          {showDate && <>{weekday(item.plannedDate)} {formatDate(item.plannedDate)} · </>}L{item.levelNumber} · {item.levelTitle} · {hoursLabel(item.plannedHours)}
        </div>
      </div>
      <span className="row plan-item-actions">
        {watch && (
          <a className="btn btn-sm btn-ghost" href={watch.url} target="_blank" rel="noopener noreferrer" title={watch.label}>
            {watch.source === 'lecture' ? <Play size={14} /> : <ExternalLink size={14} />}<span className="hide-sm">{watch.source === 'lecture' ? 'Watch' : 'Open'}</span>
          </a>
        )}
        {!item.done && <Button size="sm" variant="ghost" loading={log.isPending} onClick={() => log.mutate()} title={`Log ${hoursLabel(item.plannedHours)} of study`}><Clock size={14} /><span className="hide-sm">Log</span></Button>}
      </span>
    </div>
  );
}

function TodayView({ plan, levels, onReplan, replanning }: { plan: Plan; levels: Level[]; onReplan: () => void; replanning: boolean }) {
  const today = plan.today;
  const nextDay = plan.weeks.flatMap((w) => w.days).find((d) => d.date > today.date && d.items.some((i) => !i.done));
  const allDone = today.items.length > 0 && today.items.every((i) => i.done);
  return (
    <div className="stack">
      {plan.overdue.length > 0 && (
        <div className="card overdue-card">
          <div className="row-between">
            <span className="card-title row" style={{ gap: 8 }}><AlertTriangle size={16} /> {plan.overdue.length} topic{plan.overdue.length === 1 ? '' : 's'} from earlier days not done</span>
            <Button size="sm" variant="primary" loading={replanning} onClick={onReplan}><RefreshCw size={14} /> Catch up: re-plan from today</Button>
          </div>
          <div style={{ marginTop: 8 }}>
            {plan.overdue.slice(0, 5).map((i) => <ItemRow key={`o-${i.topicId}`} item={i} levels={levels} showDate />)}
            {plan.overdue.length > 5 && <p className="tiny subtle" style={{ marginTop: 6 }}>+{plan.overdue.length - 5} more</p>}
          </div>
        </div>
      )}
      <div className="card">
        <div className="card-head">
          <div>
            <span className="card-title">Today · {weekday(today.date, 'long')} {formatDate(today.date)}</span>
            <div className="tiny subtle">{today.items.length ? `${hoursLabel(today.plannedHours)} planned · ${hours(today.minutesLogged)} logged` : today.studyDay ? 'Nothing planned today' : 'Rest day'}</div>
          </div>
          {today.items.length > 0 && <span className="small muted">{today.items.filter((i) => i.done).length} / {today.items.length} done</span>}
        </div>
        {today.items.length === 0 ? (
          <p className="muted">
            {today.date < plan.startDate ? `Your plan starts on ${weekday(plan.startDate, 'long')} ${formatDate(plan.startDate)}.` : !today.studyDay ? 'Rest day - recharge. ' : 'Nothing planned for today. '}
            {nextDay && <>Next up: <strong>{weekday(nextDay.date, 'long')}</strong> - {nextDay.items.length} topic{nextDay.items.length === 1 ? '' : 's'}, {hoursLabel(nextDay.plannedHours)}.</>}
          </p>
        ) : (
          today.items.map((i) => <ItemRow key={`${i.topicId}-${i.plannedHours}`} item={i} levels={levels} />)
        )}
        {allDone && <p className="alert alert-success" style={{ marginTop: 12 }}>Today's plan is done. Want a head start? Tick tomorrow's first topic - the plan counts it immediately.</p>}
      </div>
      {nextDay && today.items.length > 0 && (
        <div className="card">
          <div className="card-head"><span className="card-title">Next · {weekday(nextDay.date, 'long')} {formatDate(nextDay.date)}</span><span className="small muted">{hoursLabel(nextDay.plannedHours)}</span></div>
          {nextDay.items.map((i) => <ItemRow key={`n-${i.topicId}-${i.plannedHours}`} item={i} levels={levels} />)}
        </div>
      )}
    </div>
  );
}

function WeekView({ plan, levels }: { plan: Plan; levels: Level[] }) {
  const [index, setIndex] = useState(() => Math.max(0, Math.min(plan.weeks.length - 1, plan.currentWeek - 1)));
  const week = plan.weeks[index];
  if (!week) return null;
  const today = plan.today.date;
  return (
    <div className="stack">
      <div className="row-between">
        <Button size="sm" variant="ghost" disabled={index === 0} onClick={() => setIndex(index - 1)}><ChevronLeft size={15} /> Previous</Button>
        <div style={{ textAlign: 'center' }}>
          <strong>Week {week.week} of {plan.weeks.length}</strong>
          <div className="tiny subtle">{formatDate(week.startDate)} – {formatDate(week.endDate)} · {hoursLabel(week.plannedHours)} planned · {hours(week.minutesLogged)} logged</div>
        </div>
        <Button size="sm" variant="ghost" disabled={index >= plan.weeks.length - 1} onClick={() => setIndex(index + 1)}>Next <ChevronRight size={15} /></Button>
      </div>
      <div className="week-strip">
        {week.days.map((d) => <DayCard key={d.date} day={d} isToday={d.date === today} levels={levels} />)}
      </div>
    </div>
  );
}

function DayCard({ day, isToday, levels }: { day: PlanDay; isToday: boolean; levels: Level[] }) {
  const done = day.items.filter((i) => i.done).length;
  return (
    <div className={clsx('day-card', isToday && 'today', !day.studyDay && 'rest', day.items.length > 0 && done === day.items.length && 'complete')}>
      <div className="row-between day-head">
        <span><strong>{weekday(day.date)}</strong> <span className="subtle">{formatDate(day.date)}</span></span>
        <span className="tiny subtle">{day.items.length ? hoursLabel(day.plannedHours) : day.studyDay ? '—' : 'rest'}</span>
      </div>
      {day.items.map((i) => <ItemRow key={`${i.topicId}-${i.plannedHours}`} item={i} levels={levels} />)}
    </div>
  );
}

function FullSchedule({ plan, levels }: { plan: Plan; levels: Level[] }) {
  const [open, setOpen] = useState<Set<number>>(new Set([plan.currentWeek]));
  const toggle = (w: number) => setOpen((s) => { const n = new Set(s); if (n.has(w)) n.delete(w); else n.add(w); return n; });
  return (
    <div>
      {plan.weeks.map((w) => {
        const unique = new Map(w.items.map((i) => [i.topicId, i]));
        const done = [...unique.values()].filter((i) => i.done).length;
        return (
          <Accordion
            key={w.week}
            open={open.has(w.week)}
            onToggle={() => toggle(w.week)}
            leading={<span className={clsx('timeline-dot', w.week < plan.currentWeek && 'past', w.week === plan.currentWeek && 'current')} />}
            title={<>Week {w.week}{w.week === plan.currentWeek && <span className="badge badge-accent" style={{ marginLeft: 8 }}>This week</span>}</>}
            subtitle={`${formatDate(w.startDate)} – ${formatDate(w.endDate)} · ${hoursLabel(w.plannedHours)} planned · ${hours(w.minutesLogged)} logged`}
            meta={<><Progress value={pct(done, unique.size)} /><span className="acc-count">{done} / {unique.size}</span></>}
          >
            <div style={{ marginTop: 6 }}>
              {w.days.filter((d) => d.items.length).map((d) => (
                <div key={d.date} className="schedule-day">
                  <div className="tiny subtle schedule-day-head">{weekday(d.date, 'long')} · {formatDate(d.date)} · {hoursLabel(d.plannedHours)}</div>
                  {d.items.map((i) => <ItemRow key={`${i.topicId}-${i.plannedHours}`} item={i} levels={levels} />)}
                </div>
              ))}
            </div>
          </Accordion>
        );
      })}
    </div>
  );
}

// =====================================================================================================
// Activity
// =====================================================================================================

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
        <div className="card-head"><span className="card-title">Recent sessions</span><span className="small subtle">use Log on a topic or the dashboard</span></div>
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
