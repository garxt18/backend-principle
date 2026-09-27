import { useMutation, useQueryClient } from '@tanstack/react-query';
import { ArrowRight, BookOpen, CheckCircle2, Clock, ExternalLink, Flame, Hammer, Layers, Play, Target } from 'lucide-react';
import { useMemo, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { Heatmap } from '../components/Heatmap';
import { Youtube } from '../components/icons';
import { Button, Check, ErrorState, Field, PageSkeleton, Progress, Ring } from '../components/ui';
import { api } from '../lib/api';
import { useAuth } from '../lib/auth';
import { formatDate, hours, pct, todayIso } from '../lib/format';
import { STRIVER_A2Z, STRIVER_PLAYLIST, followedFor, watchFor } from '../lib/learn';
import { keys, useHeatmap, useMyResources, usePlan, useProgress, useRoadmap, useSetTopicStatus } from '../lib/queries';
import { useToast } from '../lib/toast';
import type { Plan } from '../lib/types';

export default function Dashboard() {
  const { user } = useAuth();
  const roadmap = useRoadmap();
  const progress = useProgress();
  const plan = usePlan();
  const mine = useMyResources();
  const heat = useHeatmap();
  const setStatus = useSetTopicStatus();

  const next = useMemo(() => {
    if (!roadmap.data || !progress.data) return null;
    const done = new Set(progress.data.topics.filter((t) => t.status === 'DONE').map((t) => t.topicId));
    for (const level of roadmap.data) {
      const topic = level.topics.find((t) => !done.has(t.id));
      if (topic) return { topic, level, levelDone: level.topics.filter((t) => done.has(t.id)).length };
    }
    return null;
  }, [roadmap.data, progress.data]);

  if (roadmap.isPending || progress.isPending) return <div className="container"><PageSkeleton /></div>;
  if (progress.error || roadmap.error) return <div className="container page"><ErrorState error={progress.error ?? roadmap.error} /></div>;
  const p = progress.data!;
  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';
  const watch = next ? watchFor(next.level, next.topic, mine.data) : null;
  const followed = next ? followedFor(next.level, mine.data) : null;
  const levelPct = next ? pct(next.levelDone, next.level.topics.length) : 100;

  return (
    <div className="container page">
      <div className="page-header greeting">
        <div>
          <div className="eyebrow">Dashboard</div>
          <h1>{greeting}, <span>{user?.displayName.split(' ')[0]}</span></h1>
          <p>Small daily progress beats weekend marathons. Here is what matters today.</p>
        </div>
      </div>

      {next ? (
        <div className="card continue-card">
          <div className="continue-main">
            <div className="row" style={{ gap: 6, marginBottom: 8 }}>
              <span className="badge badge-accent">Level {next.level.levelNumber} · {next.level.title}</span>
              {next.topic.lectureNumber && <span className="badge">Lecture {next.topic.lectureNumber}</span>}
            </div>
            <div className="tiny subtle" style={{ textTransform: 'uppercase', letterSpacing: '0.06em', fontWeight: 600 }}>Continue with</div>
            <h2 className="continue-title">{next.topic.title}</h2>
            {next.topic.description && <p className="muted small" style={{ marginTop: 4 }}>{next.topic.description}</p>}
            <div className="row" style={{ marginTop: 14 }}>
              {watch && (
                <a className="btn btn-primary" href={watch.url} target="_blank" rel="noopener noreferrer">
                  {watch.source === 'lecture' ? <Play size={15} /> : <ExternalLink size={15} />} {watch.source === 'lecture' ? `Watch lecture ${next.topic.lectureNumber}` : `Open: ${watch.label}`}
                </a>
              )}
              <Button variant="outline" loading={setStatus.isPending} onClick={() => setStatus.mutate({ topicId: next.topic.id, status: 'DONE' })}>
                <CheckCircle2 size={15} /> Mark done
              </Button>
              <Link to={`/roadmap#level-${next.level.levelNumber}`} className="btn btn-ghost">Open in roadmap <ArrowRight size={15} /></Link>
            </div>
            {followed ? (
              <p className="tiny subtle" style={{ marginTop: 10 }}>
                Following <a className="link" href={followed.url} target="_blank" rel="noopener noreferrer">{followed.title}</a>{followed.channel ? ` · ${followed.channel}` : ''}
                {watch?.source === 'playlist' && ' - this lecture\'s exact link was not found, so it opens the playlist (add your own link from the roadmap).'}
              </p>
            ) : (
              <p className="tiny subtle" style={{ marginTop: 10 }}>No resource chosen for this level yet - <Link className="link" to={`/roadmap#level-${next.level.levelNumber}`}>pick the one you will follow</Link>.</p>
            )}
          </div>
          <div className="continue-side">
            <Ring value={levelPct} size={92}><div><strong>{levelPct}%</strong><div className="tiny subtle">level</div></div></Ring>
            <span className="small muted">{next.levelDone} / {next.level.topics.length} topics</span>
          </div>
        </div>
      ) : (
        <div className="card continue-card"><div className="continue-main"><h2 className="continue-title">Roadmap complete 🎉</h2><p className="muted">Every topic is done. Time for projects and interviews.</p></div></div>
      )}

      <div className="grid grid-4 stagger" style={{ marginTop: 14 }}>
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
          <span className="stat-label"><Layers size={14} /> Hours</span>
          <span className="stat-value">{p.hoursDone}<span className="subtle" style={{ fontSize: 14, fontWeight: 500 }}> / {p.hoursTotal} h</span></span>
          <span className="stat-sub">{p.percent}% of the estimated roadmap time</span>
        </div>
      </div>

      <div className="grid grid-2" style={{ marginTop: 14 }}>
        <ThisWeek plan={plan.data ?? null} loading={plan.isPending} />
        <div className="card">
          <div className="card-head">
            <span className="card-title">Levels</span>
            <Link to="/roadmap" className="link small">Roadmap</Link>
          </div>
          <div className="stack" style={{ gap: 10 }}>
            {p.levels.filter((l) => l.topicsDone > 0 || l.levelNumber <= (next?.level.levelNumber ?? 99)).slice(0, 6).map((l) => (
              <div key={l.levelId}>
                <div className="row-between small"><span className="truncate"><span className="subtle">L{l.levelNumber}</span> {l.title}</span><span className="muted nowrap">{l.topicsDone}/{l.topicsTotal}</span></div>
                <Progress value={l.percent} tone={l.percent === 100 ? 'success' : undefined} className="progress-thin" />
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="grid grid-2" style={{ marginTop: 14 }}>
        <div className="card dsa-card">
          <span className="card-title">DSA practice</span>
          <p className="muted small" style={{ margin: '6px 0 14px' }}>
            Follow Striver&apos;s A2Z sheet on takeUforward directly - it already tracks your solved problems, so there is no copy of it here.
          </p>
          <div className="row">
            <a className="btn btn-primary" href={STRIVER_A2Z} target="_blank" rel="noopener noreferrer"><Target size={15} /> Open Striver&apos;s A2Z sheet</a>
            <a className="btn btn-outline" href={STRIVER_PLAYLIST} target="_blank" rel="noopener noreferrer"><Youtube size={15} /> A2Z video playlist</a>
          </div>
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
        <p className="muted">No plan yet. Planly turns the roadmap and your available hours into a week-by-week schedule.</p>
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
      {week.items.slice(0, 8).map((item) => (
        <div key={`${item.topicId}-${item.plannedHours}`} className={`plan-item ${item.done ? 'done' : ''}`}>
          <Check checked={item.done} label={`Mark ${item.topicTitle} done`} onChange={(v) => setStatus.mutate({ topicId: item.topicId, status: v ? 'DONE' : 'IN_PROGRESS' })} />
          <span className="pi-title grow truncate">{item.topicTitle}</span>
          <span className="badge">L{item.levelNumber}</span>
          <span className="small subtle nowrap">{item.plannedHours}h</span>
        </div>
      ))}
      {week.items.length > 8 && <Link to="/planly" className="link small" style={{ display: 'inline-block', marginTop: 8 }}>+{week.items.length - 8} more this week</Link>}
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
          <option value="">Any topic / DSA practice</option>
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
