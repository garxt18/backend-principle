import { clsx } from 'clsx';
import { ExternalLink, Search, Star } from 'lucide-react';
import { Youtube } from '../components/icons';
import { useMemo, useState } from 'react';
import { Accordion, Button, Check, Chips, ErrorState, LangBadge, PageSkeleton, Progress } from '../components/ui';
import { useAuth } from '../lib/auth';
import { youtubeSearch } from '../lib/format';
import { useProgress, useRoadmap, useSetTopicStatus } from '../lib/queries';
import { useToast } from '../lib/toast';
import type { Level, Resource, Topic, TopicProgress, TopicStatus } from '../lib/types';

type LangFilter = 'ALL' | 'HI' | 'EN';
const KIND: Record<string, string> = { PLAYLIST: 'Playlist', VIDEO: 'Video', CHANNEL: 'Channel', COURSE: 'Course', DOCS: 'Docs', SEARCH: 'YouTube search' };

export default function Roadmap() {
  const { user } = useAuth();
  const roadmap = useRoadmap();
  const progress = useProgress();
  const [open, setOpen] = useState<Set<number>>(new Set());
  const [lang, setLang] = useState<LangFilter>(user?.preferredLanguage === 'HINDI' ? 'HI' : user?.preferredLanguage === 'ENGLISH' ? 'EN' : 'ALL');

  const byTopic = useMemo(() => new Map((progress.data?.topics ?? []).map((t) => [t.topicId, t])), [progress.data]);

  if (roadmap.isPending || progress.isPending) return <div className="container"><PageSkeleton /></div>;
  if (roadmap.error || progress.error) return <div className="container page"><ErrorState error={roadmap.error ?? progress.error} /></div>;

  const levels = roadmap.data!;
  const summary = new Map(progress.data!.levels.map((l) => [l.levelId, l]));
  const toggle = (id: number) => setOpen((s) => { const n = new Set(s); if (n.has(id)) n.delete(id); else n.add(id); return n; });

  return (
    <div className="container page">
      <div className="page-header">
        <div>
          <div className="eyebrow">Backend Engineering Roadmap 2026</div>
          <h1>Roadmap</h1>
          <p>Java → Spring Boot → REST → SQL → Security → Testing → Redis → Kafka → Microservices → Docker → Cloud → Kubernetes → System Design → Projects.</p>
        </div>
        <div className="row">
          <Chips<LangFilter> label="Resource language" value={lang} onChange={setLang} options={[{ value: 'ALL', label: 'All' }, { value: 'HI', label: 'Hindi' }, { value: 'EN', label: 'English' }]} />
          <Button variant="ghost" size="sm" onClick={() => setOpen(open.size ? new Set() : new Set(levels.map((l) => l.id)))}>{open.size ? 'Collapse all' : 'Expand all'}</Button>
        </div>
      </div>

      <div className="card" style={{ marginBottom: 16 }}>
        <div className="row-between small"><span className="muted">Overall · {progress.data!.topicsDone} of {progress.data!.topicsTotal} topics</span><strong>{progress.data!.percent}%</strong></div>
        <Progress value={progress.data!.percent} className="progress-thin" />
      </div>

      {levels.map((level) => {
        const lp = summary.get(level.id);
        return (
          <Accordion
            key={level.id}
            open={open.has(level.id)}
            onToggle={() => toggle(level.id)}
            leading={<span className={clsx('level-num', lp && lp.topicsDone === lp.topicsTotal && 'complete')}>{level.levelNumber}</span>}
            title={level.title}
            subtitle={`Month ${level.suggestedMonth ?? '–'} · ${level.totalHours}h · ${level.topics.length} topics`}
            meta={<><Progress value={lp?.percent ?? 0} /><span className="acc-count">{lp?.topicsDone ?? 0} / {level.topics.length}</span></>}
          >
            <LevelBody level={level} lang={lang} byTopic={byTopic} />
          </Accordion>
        );
      })}
    </div>
  );
}

function LevelBody({ level, lang, byTopic }: { level: Level; lang: LangFilter; byTopic: Map<number, TopicProgress> }) {
  const resources = level.resources.filter((r) => lang === 'ALL' || r.language === lang);
  return (
    <div className="level-body-grid">
      <div>
        <p className="muted" style={{ marginTop: 12 }}>{level.summary}</p>
        {level.projectTitle && (
          <div className="project-callout small">
            <strong>Project · {level.projectTitle}</strong>
            <div className="muted" style={{ marginTop: 4 }}>{level.projectDescription}</div>
          </div>
        )}
        <div>
          {level.topics.map((t) => <TopicRow key={t.id} topic={t} progress={byTopic.get(t.id)} />)}
        </div>
      </div>
      <aside>
        <h3 style={{ marginTop: 12 }}>Resources</h3>
        {level.whyItMatters && <p className="small subtle" style={{ marginTop: 4 }}>{level.whyItMatters}</p>}
        <div className="resource-list">
          {resources.length ? resources.map((r) => <ResourceLink key={r.id} r={r} />) : <p className="small muted">No resources in this language - switch the filter.</p>}
        </div>
      </aside>
    </div>
  );
}

function ResourceLink({ r }: { r: Resource }) {
  const Icon = r.kind === 'SEARCH' ? Search : r.kind === 'DOCS' ? ExternalLink : Youtube;
  return (
    <a className="resource" href={r.url} target="_blank" rel="noopener noreferrer">
      <Icon size={17} style={{ flex: 'none', color: r.kind === 'DOCS' || r.kind === 'SEARCH' ? 'var(--text-3)' : '#ff3b3b' }} />
      <span className="grow">
        <span className="r-title" style={{ display: 'block' }}>{r.title}</span>
        <span className="r-meta">{[KIND[r.kind], r.channel].filter(Boolean).join(' · ')}{r.note ? ` · ${r.note}` : ''}</span>
      </span>
      <span className="row" style={{ flexWrap: 'nowrap', gap: 6 }}>
        {r.primaryPick && <Star size={14} fill="var(--warning)" color="var(--warning)" aria-label="Recommended" />}
        <LangBadge lang={r.language} />
      </span>
    </a>
  );
}

function TopicRow({ topic, progress }: { topic: Topic; progress?: TopicProgress }) {
  const setStatus = useSetTopicStatus();
  const toast = useToast();
  const [notesOpen, setNotesOpen] = useState(false);
  const [notes, setNotes] = useState(progress?.notes ?? '');
  const status = progress?.status ?? 'NOT_STARTED';
  const change = (next: TopicStatus) => setStatus.mutate({ topicId: topic.id, status: next }, { onError: (e) => toast(e.message, 'error') });

  return (
    <div className={clsx('topic-row', status === 'DONE' && 'done')}>
      <span style={{ marginTop: 2 }}>
        <Check checked={status === 'DONE'} label={`Mark ${topic.title} done`} onChange={(v) => change(v ? 'DONE' : 'IN_PROGRESS')} />
      </span>
      <div style={{ minWidth: 0 }}>
        <div className="t-title">{topic.title}</div>
        {topic.description && <div className="t-desc">{topic.description}</div>}
        {topic.practice && <div className="t-desc"><span style={{ color: 'var(--accent)', fontWeight: 600 }}>Practice · </span>{topic.practice}</div>}
        <div className="t-links">
          <span className="subtle">{topic.estimatedHours}h</span>
          <a href={youtubeSearch(topic.title, true)} target="_blank" rel="noopener noreferrer"><Youtube size={13} /> Hindi</a>
          <a href={youtubeSearch(`${topic.title} java`, false)} target="_blank" rel="noopener noreferrer"><Youtube size={13} /> English</a>
          <button style={{ border: 0, background: 'none', padding: 0, color: 'var(--text-2)', fontSize: 12.5 }} onClick={() => setNotesOpen((o) => !o)}>
            {progress?.notes ? 'Notes ✓' : '+ Notes'}
          </button>
        </div>
        {notesOpen && (
          <div className="stack" style={{ marginTop: 8, animation: 'fade-up var(--dur) var(--ease-out)' }}>
            <textarea className="textarea" rows={3} maxLength={10000} value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="Key points, doubts, links..." />
            <div>
              <Button size="sm" variant="primary" loading={setStatus.isPending} onClick={() => setStatus.mutate({ topicId: topic.id, notes }, { onSuccess: () => toast('Notes saved') })}>Save notes</Button>
            </div>
          </div>
        )}
      </div>
      <select className="select status-pill" aria-label={`Status of ${topic.title}`} value={status} onChange={(e) => change(e.target.value as TopicStatus)}>
        <option value="NOT_STARTED">Not started</option>
        <option value="IN_PROGRESS">In progress</option>
        <option value="DONE">Done</option>
      </select>
    </div>
  );
}
