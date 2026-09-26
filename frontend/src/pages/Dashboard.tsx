import { useMutation, useQueryClient } from '@tanstack/react-query';
import { ArrowRight, BookOpen, Clock, ExternalLink, Flame, Hammer, ListChecks, Target } from 'lucide-react';
import { useMemo, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { Heatmap } from '../components/Heatmap';
import { Button, Check, Difficulty, ErrorState, Field, PageSkeleton, Progress, Ring } from '../components/ui';
import { api } from '../lib/api';
import { useAuth } from '../lib/auth';
import { formatDate, hours, pct, todayIso } from '../lib/format';
import { keys, useDsaSheet, useHeatmap, usePlan, useProgress, useRoadmap, useSetTopicStatus, useUpdateProblem } from '../lib/queries';
import { useToast } from '../lib/toast';
import type { Plan } from '../lib/types';

export default function Dashboard() {
  const { user } = useAuth();
  const roadmap = useRoadmap();
  const progress = useProgress();
  const plan = usePlan();
  const dsa = useDsaSheet();
  const heat = useHeatmap();
  const updateProblem = useUpdateProblem();

  const nextTopic = useMemo(() => {
    if (!roadmap.data || !progress.data) return null;
    const done = new Set(progress.data.topics.filter((t) => t.status === 'DONE').map((t) => t.topicId));
    for (const level of roadmap.data) for (const t of level.topics) if (!done.has(t.id)) return { ...t, level };
    return null;
  }, [roadmap.data, progress.data]);

  const picks = useMemo(() => (dsa.data ? dsa.data.topics.flatMap((t) => t.problems.map((p) => ({ ...p, topic: t.title }))).filter((p) => !p.solved).slice(0, 4) : []), [dsa.data]);

  if (roadmap.isPending || progress.isPending || dsa.isPending) return <div className="container"><PageSkeleton /></div>;
  if (progress.error || dsa.error) return <div className="container page"><ErrorState error={progress.error ?? dsa.error} /></div>;
  const p = progress.data!;
  const s = dsa.data!.stats;
  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';

  return (
    <div className="container page">
      <div className="page-header greeting">
        <div>
          <div className="eyebrow">Dashboard</div>
          <h1>{greeting}, <span>{user?.displayName.split(' ')[0]}</span></h1>
          <p>Small daily progress beats weekend marathons. Here is what matters today.</p>
        </div>
        {nextTopic && (
          <Link to="/roadmap" className="btn btn-primary">
            Next: {nextTopic.title.length > 34 ? `${nextTopic.title.slice(0, 34)}…` : nextTopic.title} <ArrowRight size={16} />
          </Link>
        )}
      </div>

      <div className="grid grid-4 stagger">
        <div className="card stat-card">
          <span className="stat-label"><Flame size={14} /> Streak</span>
          <span className="stat-value">{p.currentStreakDays}<span className="subtle" style={{ fontSize: 14, fontWeight: 500 }}> days</span></span>
          <span className="stat-sub">Log time daily to keep it alive</span>
        </div>
        <div className="card stat-card">
          <span className="stat-label"><Clock size={14} /> This week</span>
          <span className="stat-value">{hours(p.minutesLast7Days)}</span>
          <span className="stat-sub">{p.minutesLast7Days} minutes in the last 7 days</span>
        </div>
        <div className="card stat-card">
          <span className="stat-label"><BookOpen size={14} /> Roadmap</span>
          <span className="stat-value">{p.topicsDone}<span className="subtle" style={{ fontSize: 14, fontWeight: 500 }}> / {p.topicsTotal}</span></span>
          <Progress value={p.percent} />
        </div>
        <div className="card stat-card">
          <span className="stat-label"><ListChecks size={14} /> DSA</span>
          <span className="stat-value">{s.solved}<span className="subtle" style={{ fontSize: 14, fontWeight: 500 }}> / {s.total}</span></span>
          <span className="stat-sub">{s.solvedLast7Days} solved in the last 7 days</span>
        </div>
      </div>

      <div className="grid grid-2" style={{ marginTop: 14 }}>
        <ThisWeek plan={plan.data ?? null} loading={plan.isPending} />
        <div className="card">
          <div className="card-head">
            <span className="card-title">Overall progress</span>
          </div>
          <div className="rings">
            <div className="ring-item">
              <Ring value={p.percent}><div><strong>{p.percent}%</strong><div className="tiny subtle">roadmap</div></div></Ring>
              <span className="small muted">{p.hoursDone} / {p.hoursTotal} h</span>
            </div>
            <div className="ring-item">
              <Ring value={pct(s.solved, s.total)} color="var(--success)"><div><strong>{pct(s.solved, s.total)}%</strong><div className="tiny subtle">DSA</div></div></Ring>
              <span className="small muted">{s.revision} marked for revision</span>
            </div>
          </div>
          <div className="divider" />
          <div className="diff-bar">
            {([['Easy', s.easy, 'var(--easy)'], ['Medium', s.medium, 'var(--medium)'], ['Hard', s.hard, 'var(--hard)']] as const).map(([label, c, color]) => (
              <div key={label}>
                <div className="row-between"><span style={{ color }}>{label}</span><span className="muted">{c.solved} / {c.total}</span></div>
                <div className="progress progress-thin" style={{ marginTop: 6 }}><span style={{ width: `${pct(c.solved, c.total)}%`, background: color }} /></div>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="grid grid-2" style={{ marginTop: 14 }}>
        <div className="card">
          <div className="card-head">
            <span className="card-title">Today's DSA picks</span>
            <Link to="/dsa" className="link small">Open sheet</Link>
          </div>
          {picks.length === 0 ? (
            <p className="muted">You solved the whole sheet. Time to revise your starred problems!</p>
          ) : (
            picks.map((q) => (
              <div key={q.id} className="pick">
                <Check checked={q.solved} label={`Mark ${q.title} solved`} onChange={(v) => updateProblem.mutate({ id: q.id, solved: v })} />
                <div style={{ minWidth: 0 }}>
                  <div className="truncate" style={{ fontWeight: 550 }}>{q.title}</div>
                  <div className="tiny subtle">{q.topic}</div>
                </div>
                <span className="row" style={{ flexWrap: 'nowrap' }}>
                  <Difficulty value={q.difficulty} />
                  <a className="icon-link lc" href={q.url} target="_blank" rel="noopener noreferrer" aria-label={`Solve ${q.title} on LeetCode`}><ExternalLink size={15} /></a>
                </span>
              </div>
            ))
          )}
        </div>
        <LogTime />
      </div>

      <div className="card" style={{ marginTop: 14 }}>
        <div className="card-head">
          <span className="card-title">Consistency</span>
          <Link to="/lab" className="btn btn-sm btn-outline"><Hammer size={14} /> Continue in Rebuild Lab</Link>
        </div>
        {heat.data ? <Heatmap days={heat.data} /> : null}
      </div>
    </div>
  );
}

function ThisWeek({ plan, loading }: { plan: Plan | null; loading: boolean }) {
  const setStatus = useSetTopicStatus();
  if (loading) return <div className="card"><div className="skeleton" style={{ height: 180 }} /></div>;
  if (!plan) {
    return (
      <div className="card stack">
        <span className="card-title">This week</span>
        <p className="muted">No plan yet. Planly turns the roadmap and your available hours into a week-by-week schedule - with a DSA target.</p>
        <div><Link to="/planly" className="btn btn-primary"><Target size={16} /> Create my plan</Link></div>
      </div>
    );
  }
  const week = plan.weeks.find((w) => w.week === plan.currentWeek) ?? plan.weeks[0]!;
  const delta = plan.scheduleDeltaHours;
  return (
    <div className="card">
      <div className="card-head">
        <div>
          <span className="card-title">Week {week.week} of {plan.totalWeeks}</span>
          <div className="tiny subtle">{formatDate(week.startDate)} – {formatDate(week.endDate)}</div>
        </div>
        <span className={`badge ${delta < 0 ? 'badge-danger' : 'badge-success'}`}>{delta > 0 ? `${delta}h ahead` : delta < 0 ? `${-delta}h behind` : 'On track'}</span>
      </div>
      {week.items.map((item) => (
        <div key={`${item.topicId}-${item.plannedHours}`} className={`plan-item ${item.done ? 'done' : ''}`}>
          <Check checked={item.done} label={`Mark ${item.topicTitle} done`} onChange={(v) => setStatus.mutate({ topicId: item.topicId, status: v ? 'DONE' : 'IN_PROGRESS' })} />
          <span className="pi-title grow truncate">{item.topicTitle}</span>
          <span className="badge">L{item.levelNumber}</span>
          <span className="small subtle nowrap">{item.plannedHours}h</span>
        </div>
      ))}
      {week.dsaTarget > 0 && (
        <div style={{ marginTop: 12 }}>
          <div className="row-between small"><span className="muted">DSA this week</span><span>{week.dsaSolved} / {week.dsaTarget}</span></div>
          <Progress value={pct(week.dsaSolved, week.dsaTarget)} tone="success" className="progress-thin" />
        </div>
      )}
    </div>
  );
}

function LogTime() {
  const qc = useQueryClient();
  const toast = useToast();
  const roadmap = useRoadmap();
  const [minutes, setMinutes] = useState(60);
  const [date, setDate] = useState(todayIso());
  const [topicId, setTopicId] = useState('');
  const [note, setNote] = useState('');
  const log = useMutation({
    mutationFn: () => api('/api/sessions', { method: 'POST', body: { minutes, date, topicId: topicId ? Number(topicId) : null, note: note || null } }),
    onSuccess: () => {
      toast(`Logged ${minutes} minutes. Keep going!`);
      setNote('');
      [keys.progress, keys.heatmap, keys.sessions, keys.plan].forEach((k) => qc.invalidateQueries({ queryKey: k }));
    },
    onError: (e) => toast(e.message, 'error'),
  });
  const submit = (e: FormEvent) => {
    e.preventDefault();
    log.mutate();
  };
  return (
    <form className="card stack" onSubmit={submit}>
      <span className="card-title">Log study time</span>
      <div className="grid" style={{ gridTemplateColumns: '1fr 1fr' }}>
        <Field label="Minutes"><input className="input" type="number" min={1} max={720} value={minutes} onChange={(e) => setMinutes(Number(e.target.value))} required /></Field>
        <Field label="Date"><input className="input" type="date" value={date} max={todayIso()} onChange={(e) => setDate(e.target.value)} required /></Field>
      </div>
      <Field label="Topic (optional)">
        <select className="select" value={topicId} onChange={(e) => setTopicId(e.target.value)}>
          <option value="">Any topic / DSA</option>
          {roadmap.data?.map((l) => (
            <optgroup key={l.id} label={`L${l.levelNumber} · ${l.title}`}>
              {l.topics.map((t) => <option key={t.id} value={t.id}>{t.title}</option>)}
            </optgroup>
          ))}
        </select>
      </Field>
      <Field label="What did you learn?"><input className="input" value={note} maxLength={500} onChange={(e) => setNote(e.target.value)} placeholder="e.g. @Transactional rollback rules" /></Field>
      <div><Button type="submit" variant="primary" loading={log.isPending}>Log time</Button></div>
    </form>
  );
}
