import { useQuery, useQueryClient } from '@tanstack/react-query';
import { clsx } from 'clsx';
import { ArrowLeft, ArrowRight, BookOpenText, Eye, EyeOff, Keyboard, PartyPopper, RotateCcw, Search, SkipForward, Undo2 } from 'lucide-react';
import { useCallback, useEffect, useMemo, useRef, useState, type KeyboardEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button, Chips, ErrorState, PageSkeleton, Progress } from '../../components/ui';
import { api } from '../../lib/api';
import { keys } from '../../lib/queries';
import { useToast } from '../../lib/toast';
import type { Explanation, LabFile } from '../../lib/types';

type Mode = 'type' | 'read';
const normalize = (s: string) => s.trim().replace(/\s+/g, ' ');

export default function LabEditor() {
  const { fileId = '' } = useParams();
  const file = useQuery({ queryKey: keys.labFile(fileId), queryFn: () => api<LabFile>(`/api/lab/files/${fileId}`), staleTime: 0, gcTime: 0 });
  if (file.isPending) return <div className="container"><PageSkeleton /></div>;
  if (file.error) return <div className="container page"><ErrorState error={file.error} /></div>;
  return <Editor key={fileId} file={file.data!} />;
}

function Editor({ file }: { file: LabFile }) {
  const qc = useQueryClient();
  const toast = useToast();
  const lines = file.lines;
  const skipBlank = useCallback((i: number) => {
    let j = i;
    while (j < lines.length && lines[j]!.trim() === '') j++;
    return j;
  }, [lines]);

  const [mode, setMode] = useState<Mode>('type');
  const [blind, setBlind] = useState(false);
  const [cursor, setCursor] = useState(() => skipBlank(Math.min(file.linesCompleted, lines.length)));
  const [selected, setSelected] = useState(() => Math.min(skipBlank(Math.min(file.linesCompleted, lines.length)), Math.max(lines.length - 1, 0)));
  const [typed, setTyped] = useState('');
  const [shake, setShake] = useState(false);
  const [notes, setNotes] = useState<Record<string, string>>(file.notes);

  // ---- progress saving: debounced while typing, flushed when leaving the page
  const saved = useRef(file.linesCompleted);
  const latest = useRef(cursor);
  latest.current = cursor;
  const save = useCallback(async () => {
    if (latest.current === saved.current) return;
    const value = latest.current;
    try {
      await api(`/api/lab/files/${file.id}/progress`, { method: 'PUT', body: { linesCompleted: value } });
      saved.current = value;
    } catch (e) {
      toast(`Progress not saved: ${(e as Error).message}`, 'error');
    }
  }, [file.id, toast]);
  useEffect(() => {
    const t = window.setTimeout(save, 700);
    return () => window.clearTimeout(t);
  }, [cursor, save]);
  useEffect(() => () => {
    void save().then(() => qc.invalidateQueries({ queryKey: keys.labProject(file.projectId) }));
  }, [save, qc, file.projectId]);

  const inputRef = useRef<HTMLInputElement>(null);
  const codeRef = useRef<HTMLDivElement>(null);
  const done = cursor >= lines.length;
  const target = lines[cursor] ?? '';
  const want = normalize(target);
  const got = normalize(typed);
  let ok = 0;
  while (ok < got.length && ok < want.length && got[ok] === want[ok]) ok++;
  const matched = got === want && !done;

  const advance = useCallback(() => {
    const next = skipBlank(cursor + 1);
    setCursor(next);
    setSelected(Math.min(next, lines.length - 1));
    setTyped('');
  }, [cursor, skipBlank, lines.length]);

  const back = () => {
    let prev = cursor - 1;
    while (prev > 0 && lines[prev]!.trim() === '') prev--;
    prev = Math.max(0, prev);
    setCursor(prev);
    setSelected(prev);
    setTyped('');
  };

  const onKey = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      if (matched) advance();
      else {
        setShake(true);
        window.setTimeout(() => setShake(false), 340);
      }
    } else if (e.key === 'Escape') {
      setBlind((b) => !b);
    }
  };

  // keep the active line in view
  useEffect(() => {
    codeRef.current?.querySelector<HTMLElement>(`[data-line="${selected}"]`)?.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
  }, [selected]);
  useEffect(() => {
    if (mode === 'type') inputRef.current?.focus();
  }, [mode, cursor]);

  // read mode: arrow keys move through lines
  useEffect(() => {
    if (mode !== 'read') return;
    const onDocKey = (e: globalThis.KeyboardEvent) => {
      if ((e.target as HTMLElement).closest('textarea, input')) return;
      if (e.key === 'ArrowDown' || e.key === 'j') { e.preventDefault(); setSelected((s) => Math.min(lines.length - 1, s + 1)); }
      if (e.key === 'ArrowUp' || e.key === 'k') { e.preventDefault(); setSelected((s) => Math.max(0, s - 1)); }
    };
    document.addEventListener('keydown', onDocKey);
    return () => document.removeEventListener('keydown', onDocKey);
  }, [mode, lines.length]);

  const percent = lines.length ? Math.round((cursor * 100) / lines.length) : 100;

  return (
    <div className="container page">
      <div className="row-between" style={{ marginBottom: 14 }}>
        <Link to={`/lab/p/${file.projectId}`} className="small muted row"><ArrowLeft size={14} /> Project overview</Link>
        <div className="row">
          {file.previousFileId && <Link to={`/lab/f/${file.previousFileId}`} className="btn btn-sm btn-ghost"><ArrowLeft size={14} /> Prev file</Link>}
          {file.nextFileId && <Link to={`/lab/f/${file.nextFileId}`} className="btn btn-sm btn-outline">Next file <ArrowRight size={14} /></Link>}
        </div>
      </div>
      <div className="page-header" style={{ marginBottom: 16 }}>
        <div style={{ minWidth: 0 }}>
          <div className="row" style={{ marginBottom: 6 }}><span className="badge badge-accent">Step {file.buildOrder + 1} · {file.layerLabel}</span><span className="badge">{file.language}</span></div>
          <h1 className="mono" style={{ fontSize: '1.05rem', overflowWrap: 'anywhere', fontWeight: 600 }}>{file.path}</h1>
          <p className="small">{file.layerWhy}</p>
        </div>
      </div>

      <div className="editor-layout">
        <div className="stack" style={{ minWidth: 0 }}>
          <div className="card" style={{ padding: 14 }}>
            <div className="row-between">
              <Chips<Mode> label="Mode" value={mode} onChange={setMode} options={[{ value: 'type', label: <span className="row" style={{ gap: 6 }}><Keyboard size={14} /> Type</span> }, { value: 'read', label: <span className="row" style={{ gap: 6 }}><BookOpenText size={14} /> Read</span> }]} />
              {mode === 'type' && (
                <Button size="sm" variant={blind ? 'primary' : 'outline'} onClick={() => setBlind((b) => !b)} title="Hide the line and type it from memory (Esc)">
                  {blind ? <EyeOff size={14} /> : <Eye size={14} />} {blind ? 'Blind mode on' : 'Blind mode'}
                </Button>
              )}
            </div>
            <div style={{ marginTop: 12 }}>
              <div className="row-between small"><span className="muted">{cursor} / {lines.length} lines</span><strong>{percent}%</strong></div>
              <Progress value={percent} tone={done ? 'success' : undefined} className="progress-thin" />
            </div>
          </div>

          {mode === 'type' && (
            done ? (
              <div className="card celebrate">
                <PartyPopper size={36} />
                <h2>File rebuilt!</h2>
                <p className="muted" style={{ margin: '6px 0 16px' }}>Before moving on, explain this file out loud in 60 seconds. Could you write it again tomorrow without looking?</p>
                <div className="row" style={{ justifyContent: 'center' }}>
                  <Button variant="outline" onClick={() => { const s = skipBlank(0); setCursor(s); setSelected(s); }}><RotateCcw size={14} /> Retype in blind mode</Button>
                  {file.nextFileId ? <Link to={`/lab/f/${file.nextFileId}`} className="btn btn-primary">Next file <ArrowRight size={15} /></Link>
                    : <Link to={`/lab/p/${file.projectId}`} className="btn btn-primary">Back to project</Link>}
                </div>
              </div>
            ) : (
              <div className="typer">
                <div className="row-between tiny subtle" style={{ marginBottom: 8 }}>
                  <span>Line {cursor + 1}</span>
                  <span className="row" style={{ gap: 6 }}><span className="kbd">Enter</span> next <span className="kbd">Esc</span> blind</span>
                </div>
                <div className="target" aria-live="polite">
                  {blind ? (
                    <span className="subtle">{matched ? 'Correct - press Enter' : `Type from memory · ${ok} characters correct so far`}</span>
                  ) : (
                    <>
                      <span className="ok">{want.slice(0, ok)}</span>
                      {ok < got.length && <span className="bad">{want.slice(ok, ok + 1) || ' '}</span>}
                      <span className="rest">{want.slice(ok < got.length ? ok + 1 : ok)}</span>
                    </>
                  )}
                </div>
                <input
                  ref={inputRef}
                  className={clsx('input type-input', matched && 'match', shake && 'shake')}
                  value={typed}
                  onChange={(e) => setTyped(e.target.value)}
                  onKeyDown={onKey}
                  autoComplete="off"
                  autoCapitalize="off"
                  autoCorrect="off"
                  spellCheck={false}
                  aria-label={`Type line ${cursor + 1}`}
                  placeholder="Type the line (indentation optional), then press Enter"
                />
                <div className="row" style={{ marginTop: 10 }}>
                  <Button size="sm" variant="ghost" onClick={advance}><SkipForward size={14} /> Skip line</Button>
                  <Button size="sm" variant="ghost" onClick={back} disabled={cursor === 0}><Undo2 size={14} /> Back</Button>
                </div>
              </div>
            )
          )}

          <div className="code" ref={codeRef} role="list" aria-label="File content">
            {lines.map((line, i) => (
              <div
                key={i}
                data-line={i}
                role="listitem"
                className={clsx(
                  'code-line',
                  mode === 'type' && i < cursor && 'done',
                  mode === 'type' && i === cursor && 'current',
                  mode === 'type' && (i > cursor || (i === cursor && blind)) && 'hidden',
                  i === selected && (mode === 'read' || i !== cursor) && 'selected',
                )}
                onClick={() => setSelected(i)}
              >
                <span className="ln">{i + 1}</span>
                <span className="src">{line || ' '}</span>
                {notes[String(i + 1)] ? <span className="has-note" title="You wrote a note here" /> : <span />}
              </div>
            ))}
          </div>
        </div>

        <aside className="side-panel stack">
          <ExplainPanel fileId={file.id} line={selected + 1} hidden={mode === 'type' && blind && selected >= cursor} />
          <NotePanel fileId={file.id} line={selected + 1} value={notes[String(selected + 1)] ?? ''} onSaved={(text) => setNotes((n) => {
            const copy = { ...n };
            if (text) copy[String(selected + 1)] = text; else delete copy[String(selected + 1)];
            return copy;
          })} />
        </aside>
      </div>
    </div>
  );
}

function ExplainPanel({ fileId, line, hidden }: { fileId: string; line: number; hidden: boolean }) {
  const explain = useQuery({
    queryKey: ['lab', 'explain', fileId, line],
    queryFn: () => api<Explanation>(`/api/lab/files/${fileId}/lines/${line}/explain`),
    staleTime: Infinity,
  });
  const ex = explain.data;
  const googleFor = useMemo(() => (term: string) => `https://www.google.com/search?q=${encodeURIComponent(`${term} java spring boot`)}`, []);
  if (hidden) {
    return <div className="card small muted">Blind mode: the explanation appears once you have typed this line. Try to remember why it exists!</div>;
  }
  return (
    <div className="card" key={line} style={{ animation: 'fade-in var(--dur) ease' }}>
      <div className="row-between" style={{ marginBottom: 10 }}>
        <span className="tiny subtle" style={{ textTransform: 'uppercase', letterSpacing: '0.06em', fontWeight: 600 }}>Line {line}{ex ? ` · ${ex.kind}` : ''}</span>
      </div>
      {explain.isPending ? (
        <div className="stack"><div className="skeleton" style={{ height: 40 }} /><div className="skeleton" style={{ height: 14 }} /><div className="skeleton" style={{ height: 14, width: '70%' }} /></div>
      ) : explain.error ? (
        <ErrorState error={explain.error} />
      ) : ex ? (
        <>
          <div className="explain-code">{ex.code.trim() || '(blank line)'}</div>
          <p style={{ marginTop: 12 }}>{ex.summary}</p>
          {ex.context && <p className="small subtle" style={{ marginTop: 6 }}>Context: {ex.context}</p>}
          {ex.notes.length > 0 && <div className="divider" />}
          {ex.notes.map((n) => (
            <div key={n.term} className="explain-note">
              <div className="note-term">
                <span>{n.term}</span>
                <a href={googleFor(n.term)} target="_blank" rel="noopener noreferrer" className="icon-link" style={{ width: 24, height: 24 }} aria-label={`Search ${n.term}`} title="Search the docs"><Search size={13} /></a>
              </div>
              <div className="note-text">{n.text}</div>
            </div>
          ))}
        </>
      ) : null}
    </div>
  );
}

function NotePanel({ fileId, line, value, onSaved }: { fileId: string; line: number; value: string; onSaved: (text: string) => void }) {
  const toast = useToast();
  const [text, setText] = useState(value);
  const [busy, setBusy] = useState(false);
  useEffect(() => setText(value), [value, line]);
  const dirty = text.trim() !== value.trim();
  const save = async () => {
    setBusy(true);
    try {
      await api(`/api/lab/files/${fileId}/lines/${line}/note`, { method: 'PUT', body: { note: text } });
      onSaved(text.trim());
      toast(text.trim() ? 'Note saved' : 'Note removed');
    } catch (e) {
      toast((e as Error).message, 'error');
    } finally {
      setBusy(false);
    }
  };
  return (
    <div className="card stack">
      <div>
        <span className="card-title">Explain it in your own words</span>
        <p className="tiny subtle" style={{ marginTop: 2 }}>Line {line} · Hindi, Hinglish or English - whatever helps you remember.</p>
      </div>
      <textarea className="textarea" rows={3} maxLength={5000} value={text} onChange={(e) => setText(e.target.value)} placeholder="Yeh line kya karti hai aur kyun zaroori hai?" />
      <div className="row">
        <Button size="sm" variant="primary" disabled={!dirty} loading={busy} onClick={save}>Save note</Button>
        {value && <span className="tiny subtle">Saved · marked with a dot in the code</span>}
      </div>
    </div>
  );
}
