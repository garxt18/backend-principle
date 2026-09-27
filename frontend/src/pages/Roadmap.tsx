import { clsx } from 'clsx';
import { Bookmark, BookmarkCheck, CalendarClock, ExternalLink, Link2, ListVideo, Play, Plus, Star, Trash2, X } from 'lucide-react';
import { useEffect, useMemo, useState, type FormEvent, type MouseEvent } from 'react';
import { useLocation } from 'react-router-dom';
import { Youtube } from '../components/icons';
import { Accordion, Button, Check, Chips, ErrorState, Field, LangBadge, PageSkeleton, Progress } from '../components/ui';
import { useAuth } from '../lib/auth';
import { followedFor, isYoutube, levelLinks, topicLinks, watchFor } from '../lib/learn';
import { useAddLink, useDeleteLink, useFollow, useMyResources, usePlannedDates, useProgress, useRoadmap, useSetTopicStatus } from '../lib/queries';
import { formatDate, weekday } from '../lib/format';
import { useToast } from '../lib/toast';
import type { Level, MyLink, MyResources, Resource, Topic, TopicProgress, TopicStatus } from '../lib/types';

type LangFilter = 'ALL' | 'HI' | 'EN';
const KIND: Record<string, string> = { PLAYLIST: 'Playlist', VIDEO: 'Video', CHANNEL: 'Channel', COURSE: 'Course', DOCS: 'Docs' };

export default function Roadmap() {
  const { user } = useAuth();
  const roadmap = useRoadmap();
  const progress = useProgress();
  const mine = useMyResources();
  const planned = usePlannedDates();
  const location = useLocation();
  const [open, setOpen] = useState<Set<number>>(new Set());
  const [lang, setLang] = useState<LangFilter>(user?.preferredLanguage === 'HINDI' ? 'HI' : user?.preferredLanguage === 'ENGLISH' ? 'EN' : 'ALL');

  const byTopic = useMemo(() => new Map((progress.data?.topics ?? []).map((t) => [t.topicId, t])), [progress.data]);

  // /roadmap#level-3 (from the dashboard) opens and scrolls to that level.
  useEffect(() => {
    const m = /^#level-(\d+)$/.exec(location.hash);
    if (!m || !roadmap.data) return;
    const level = roadmap.data.find((l) => l.levelNumber === Number(m[1]));
    if (!level) return;
    setOpen((s) => new Set(s).add(level.id));
    window.setTimeout(() => document.getElementById(`level-${level.levelNumber}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 60);
  }, [location.hash, roadmap.data]);

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
          <p>Java and Spring Boot follow your two playlists lecture by lecture. For every other level, pick the resource you will follow - or add your own.</p>
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
        const followed = followedFor(level, mine.data);
        return (
          <div key={level.id} id={`level-${level.levelNumber}`} className="level-anchor">
            <Accordion
              open={open.has(level.id)}
              onToggle={() => toggle(level.id)}
              leading={<span className={clsx('level-num', lp && lp.topicsDone === lp.topicsTotal && 'complete')}>{level.levelNumber}</span>}
              title={level.title}
              subtitle={level.playlist
                ? `${level.playlist.name} · lectures ${level.topics.find((t) => t.lectureNumber)?.lectureNumber ?? ''}-${[...level.topics].reverse().find((t) => t.lectureNumber)?.lectureNumber ?? ''} · ${level.totalHours}h`
                : `Month ${level.suggestedMonth ?? '–'} · ${level.totalHours}h · ${level.topics.length} topics${followed ? ` · following ${followed.title}` : ''}`}
              meta={<><Progress value={lp?.percent ?? 0} /><span className="acc-count">{lp?.topicsDone ?? 0} / {level.topics.length}</span></>}
            >
              <LevelBody level={level} lang={lang} byTopic={byTopic} mine={mine.data} planned={planned} />
            </Accordion>
          </div>
        );
      })}
    </div>
  );
}

function LevelBody({ level, lang, byTopic, mine, planned }: { level: Level; lang: LangFilter; byTopic: Map<number, TopicProgress>; mine: MyResources | undefined; planned: Map<number, string> }) {
  const resources = level.resources.filter((r) => lang === 'ALL' || r.language === lang || level.playlist);
  const choice = mine?.choices.find((c) => c.levelId === level.id);
  const followed = followedFor(level, mine);
  return (
    <div className="level-body-grid">
      <div>
        <p className="muted" style={{ marginTop: 12 }}>{level.summary}</p>
        {level.playlist && (
          <div className="playlist-banner">
            <ListVideo size={18} style={{ flex: 'none', color: 'var(--accent)' }} />
            <div className="grow">
              <div style={{ fontWeight: 600 }}>{level.playlist.name}</div>
              <div className="tiny subtle">{level.playlist.channel} · every topic below is one lecture, in order</div>
            </div>
            <a className="btn btn-sm btn-outline" href={level.playlist.url} target="_blank" rel="noopener noreferrer"><Youtube size={14} /> Playlist</a>
          </div>
        )}
        {level.projectTitle && (
          <div className="project-callout small">
            <strong>Project · {level.projectTitle}</strong>
            <div className="muted" style={{ marginTop: 4 }}>{level.projectDescription}</div>
          </div>
        )}
        <div>
          {level.topics.map((t) => <TopicRow key={t.id} level={level} topic={t} progress={byTopic.get(t.id)} mine={mine} plannedOn={planned.get(t.id)} />)}
        </div>
      </div>
      <aside>
        <h3 style={{ marginTop: 12 }}>{level.playlist ? 'Your playlist' : 'Resources'}</h3>
        {level.whyItMatters && <p className="small subtle" style={{ marginTop: 4 }}>{level.whyItMatters}</p>}
        {!level.playlist && (
          <p className="tiny subtle" style={{ marginTop: 8 }}>
            {followed ? <>You follow <strong>{followed.title}</strong>. The dashboard sends you there next.</> : <>Tap <Bookmark size={11} /> on the one you are going to follow.</>}
          </p>
        )}
        <div className="resource-list">
          {resources.length ? resources.map((r) => (
            <ResourceLink key={r.id} r={r} levelId={level.id} following={level.playlist ? !choice : choice?.resourceId === r.id} canFollow={!level.playlist || !!choice} />
          )) : <p className="small muted">No resources in this language - switch the filter.</p>}
        </div>
        <MyLevelLinks level={level} mine={mine} />
      </aside>
    </div>
  );
}

function FollowButton({ levelId, resourceId, userResourceId, following }: { levelId: number; resourceId?: number; userResourceId?: number; following: boolean }) {
  const follow = useFollow();
  const toast = useToast();
  const onClick = (e: MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    follow.mutate(following ? { levelId, clear: true } : { levelId, resourceId, userResourceId }, {
      onSuccess: () => toast(following ? 'Stopped following' : 'Following - the roadmap and dashboard now use this one'),
      onError: (err) => toast(err.message, 'error'),
    });
  };
  return (
    <button type="button" className={clsx('follow-btn', following && 'on')} onClick={onClick} disabled={follow.isPending} title={following ? 'You follow this one (click to undo)' : "I'm going to follow this one"} aria-pressed={following}>
      {following ? <BookmarkCheck size={15} /> : <Bookmark size={15} />}
      <span>{following ? 'Following' : 'Follow'}</span>
    </button>
  );
}

function ResourceLink({ r, levelId, following, canFollow }: { r: Resource; levelId: number; following: boolean; canFollow: boolean }) {
  const Icon = r.kind === 'DOCS' || r.kind === 'COURSE' ? ExternalLink : Youtube;
  return (
    <div className={clsx('resource', following && 'followed')}>
      <a className="resource-main" href={r.url} target="_blank" rel="noopener noreferrer">
        <Icon size={17} style={{ flex: 'none', color: r.kind === 'DOCS' || r.kind === 'COURSE' ? 'var(--text-3)' : '#ff3b3b' }} />
        <span className="grow" style={{ minWidth: 0 }}>
          <span className="r-title" style={{ display: 'block' }}>{r.title}</span>
          <span className="r-meta">{[KIND[r.kind], r.channel].filter(Boolean).join(' · ')}{r.note ? ` · ${r.note}` : ''}</span>
        </span>
        <span className="row" style={{ flexWrap: 'nowrap', gap: 6 }}>
          {r.primaryPick && <Star size={14} fill="var(--warning)" color="var(--warning)" aria-label="Recommended" />}
          <LangBadge lang={r.language} />
        </span>
      </a>
      {canFollow ? <FollowButton levelId={levelId} resourceId={r.id} following={following} /> : <span className="follow-btn on static"><BookmarkCheck size={15} /><span>Following</span></span>}
    </div>
  );
}

function MyLevelLinks({ level, mine }: { level: Level; mine: MyResources | undefined }) {
  const links = levelLinks(level.id, mine);
  const choice = mine?.choices.find((c) => c.levelId === level.id);
  const [adding, setAdding] = useState(false);
  return (
    <div style={{ marginTop: 16 }}>
      <div className="row-between">
        <span className="small" style={{ fontWeight: 600 }}>Your links</span>
        {!adding && <Button size="sm" variant="ghost" onClick={() => setAdding(true)}><Plus size={14} /> Add link</Button>}
      </div>
      {links.length === 0 && !adding && <p className="tiny subtle" style={{ marginTop: 4 }}>Found a playlist or video you like more? Add it here and follow it.</p>}
      <div className="resource-list" style={{ marginTop: 6 }}>
        {links.map((l) => <MyLinkRow key={l.id} link={l} following={choice?.userResourceId === l.id} levelId={level.id} />)}
      </div>
      {adding && <AddLinkForm levelId={level.id} onDone={() => setAdding(false)} />}
    </div>
  );
}

function MyLinkRow({ link, following, levelId }: { link: MyLink; following?: boolean; levelId?: number }) {
  const del = useDeleteLink();
  const toast = useToast();
  return (
    <div className={clsx('resource', following && 'followed')}>
      <a className="resource-main" href={link.url} target="_blank" rel="noopener noreferrer">
        {isYoutube(link.url) ? <Youtube size={17} style={{ flex: 'none', color: '#ff3b3b' }} /> : <Link2 size={17} style={{ flex: 'none', color: 'var(--text-3)' }} />}
        <span className="grow" style={{ minWidth: 0 }}>
          <span className="r-title" style={{ display: 'block' }}>{link.title}</span>
          <span className="r-meta truncate" style={{ display: 'block' }}>{link.note ?? hostOf(link.url)}</span>
        </span>
      </a>
      {levelId !== undefined && <FollowButton levelId={levelId} userResourceId={link.id} following={!!following} />}
      <button type="button" className="icon-link" aria-label={`Delete ${link.title}`} title="Delete" disabled={del.isPending}
        onClick={() => del.mutate(link.id, { onSuccess: () => toast('Link deleted'), onError: (e) => toast(e.message, 'error') })}>
        <Trash2 size={14} />
      </button>
    </div>
  );
}

function hostOf(url: string) {
  try {
    return new URL(url).hostname.replace(/^www\./, '');
  } catch {
    return url;
  }
}

function AddLinkForm({ levelId, topicId, onDone }: { levelId?: number; topicId?: number; onDone: () => void }) {
  const add = useAddLink();
  const toast = useToast();
  const [title, setTitle] = useState('');
  const [url, setUrl] = useState('');
  const submit = (e: FormEvent) => {
    e.preventDefault();
    add.mutate({ levelId, topicId, title: title.trim() || 'My link', url: url.trim() }, {
      onSuccess: () => { toast('Link added'); onDone(); },
      onError: (err) => toast(err.message, 'error'),
    });
  };
  return (
    <form className="add-link" onSubmit={submit}>
      <Field label="Link (YouTube video, playlist or any page)">
        <input className="input" type="url" required placeholder="https://www.youtube.com/watch?v=..." value={url} onChange={(e) => setUrl(e.target.value)} autoFocus />
      </Field>
      <Field label="Title">
        <input className="input" maxLength={200} placeholder={topicId ? 'e.g. Better explanation of this lecture' : 'e.g. My favourite Docker playlist'} value={title} onChange={(e) => setTitle(e.target.value)} />
      </Field>
      <div className="row">
        <Button type="submit" size="sm" variant="primary" loading={add.isPending}>Save link</Button>
        <Button type="button" size="sm" variant="ghost" onClick={onDone}><X size={14} /> Cancel</Button>
      </div>
    </form>
  );
}

function TopicRow({ level, topic, progress, mine, plannedOn }: { level: Level; topic: Topic; progress?: TopicProgress; mine: MyResources | undefined; plannedOn?: string }) {
  const setStatus = useSetTopicStatus();
  const toast = useToast();
  const [notesOpen, setNotesOpen] = useState(false);
  const [linkOpen, setLinkOpen] = useState(false);
  const [notes, setNotes] = useState(progress?.notes ?? '');
  const status = progress?.status ?? 'NOT_STARTED';
  const change = (next: TopicStatus) => setStatus.mutate({ topicId: topic.id, status: next }, { onError: (e) => toast(e.message, 'error') });
  const watch = watchFor(level, topic, mine);
  const own = topicLinks(topic.id, mine);

  return (
    <div className={clsx('topic-row', status === 'DONE' && 'done')}>
      <span style={{ marginTop: 2 }}>
        <Check checked={status === 'DONE'} label={`Mark ${topic.title} done`} onChange={(v) => change(v ? 'DONE' : 'IN_PROGRESS')} />
      </span>
      <div style={{ minWidth: 0 }}>
        <div className="t-title">
          {topic.lectureNumber && <span className="lecture-no">L{topic.lectureNumber}</span>}
          {topic.title}
        </div>
        {topic.description && <div className="t-desc">{topic.description}</div>}
        {topic.practice && <div className="t-desc"><span style={{ color: 'var(--accent)', fontWeight: 600 }}>Practice · </span>{topic.practice}</div>}
        <div className="t-links">
          <span className="subtle">{topic.estimatedHours}h</span>
          {plannedOn && status !== 'DONE' && <span className="planned-on" title="Planned in your Planly plan"><CalendarClock size={13} /> {weekday(plannedOn)} {formatDate(plannedOn)}</span>}
          {watch && (
            <a href={watch.url} target="_blank" rel="noopener noreferrer" className={clsx(watch.source === 'lecture' && 'watch-exact')}
              title={watch.source === 'playlist' ? 'The exact video of this lecture was not found - opens the playlist; add your own link for it' : undefined}>
              {watch.source === 'lecture' ? <Play size={13} /> : <ExternalLink size={13} />} {watch.source === 'lecture' ? `Watch lecture ${topic.lectureNumber}` : watch.label}
            </a>
          )}
          <button type="button" className="text-btn" onClick={() => setLinkOpen((o) => !o)}>{own.length ? `My links (${own.length})` : '+ My link'}</button>
          <button type="button" className="text-btn" onClick={() => setNotesOpen((o) => !o)}>{progress?.notes ? 'Notes ✓' : '+ Notes'}</button>
        </div>
        {linkOpen && (
          <div className="stack" style={{ marginTop: 8, animation: 'fade-up var(--dur) var(--ease-out)' }}>
            {own.map((l) => <MyLinkRow key={l.id} link={l} />)}
            <AddLinkForm topicId={topic.id} onDone={() => setLinkOpen(false)} />
          </div>
        )}
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
