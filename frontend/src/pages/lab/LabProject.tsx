import { useQuery, useQueryClient } from '@tanstack/react-query';
import { clsx } from 'clsx';
import { ArrowLeft, CheckCircle2, ChevronDown, Download, Laptop, MonitorSmartphone, Play, Server, Upload } from 'lucide-react';
import { useRef, useState, type ChangeEvent, type ReactNode } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button, ErrorState, PageSkeleton, Progress } from '../../components/ui';
import { api, downloadFile } from '../../lib/api';
import { pct } from '../../lib/format';
import { keys } from '../../lib/queries';
import { useToast } from '../../lib/toast';
import type { LabFileEntry, LabProjectDetail, SyncResult, Track } from '../../lib/types';

export default function LabProject() {
  const { projectId = '' } = useParams();
  const detail = useQuery({ queryKey: keys.labProject(projectId), queryFn: () => api<LabProjectDetail>(`/api/lab/projects/${projectId}`) });
  const [frontendOpen, setFrontendOpen] = useState(false);
  if (detail.isPending) return <div className="container"><PageSkeleton /></div>;
  if (detail.error) return <div className="container page"><ErrorState error={detail.error} /></div>;
  const d = detail.data!;
  const backend = d.files.filter((f) => f.track === 'BACKEND');
  const frontend = d.files.filter((f) => f.track === 'FRONTEND');
  const next = backend.find((f) => !f.completed) ?? frontend.find((f) => !f.completed) ?? d.files[0];

  return (
    <div className="container page">
      <Link to="/lab" className="small muted row" style={{ marginBottom: 12 }}><ArrowLeft size={14} /> Rebuild Lab</Link>
      <div className="page-header">
        <div>
          <h1>{d.project.name}</h1>
          <p>{d.project.description || `${d.project.fileCount} files · ${d.project.totalLines} lines`}</p>
        </div>
        {next && <Link to={`/lab/f/${next.id}`} className="btn btn-primary"><Play size={15} /> {d.linesCompleted ? 'Continue rebuilding' : 'Start rebuilding'}</Link>}
      </div>

      <div className="grid grid-2" style={{ marginBottom: 16 }}>
        <TrackCard icon={<Server size={15} />} title="Backend & database" files={backend} note="Main track: build files, config, SQL, entities, repositories, services, controllers, tests." />
        <TrackCard icon={<MonitorSmartphone size={15} />} title="Frontend (optional)" files={frontend} note={frontend.length ? 'Pages, components and styles - practise them after the backend if you like.' : 'This project has no frontend files.'} />
      </div>

      <SyncCard projectId={projectId} projectName={d.project.name} />

      <h2 className="section-heading"><Server size={17} /> Backend & database</h2>
      <p className="small muted" style={{ margin: '2px 0 12px' }}>Build in this order - every file only depends on files you have already typed.</p>
      <LayerList d={d} track="BACKEND" />

      {frontend.length > 0 && (
        <section className="frontend-section">
          <button type="button" className="frontend-toggle" onClick={() => setFrontendOpen((o) => !o)} aria-expanded={frontendOpen}>
            <MonitorSmartphone size={17} />
            <span className="grow" style={{ textAlign: 'left' }}>
              <span style={{ fontWeight: 650, display: 'block' }}>Frontend practice (optional)</span>
              <span className="tiny subtle">{frontend.length} files · {frontend.reduce((s, f) => s + f.lineCount, 0).toLocaleString()} lines of TSX / HTML / CSS / JS</span>
            </span>
            <ChevronDown size={18} className={clsx('acc-chevron', frontendOpen && 'rot')} />
          </button>
          {frontendOpen && <div style={{ marginTop: 12 }}><LayerList d={d} track="FRONTEND" /></div>}
        </section>
      )}
    </div>
  );
}

function TrackCard({ icon, title, files, note }: { icon: ReactNode; title: string; files: LabFileEntry[]; note: string }) {
  const lines = files.reduce((s, f) => s + f.lineCount, 0);
  const done = files.reduce((s, f) => s + f.linesCompleted, 0);
  const filesDone = files.filter((f) => f.completed).length;
  return (
    <div className="card stat-card">
      <span className="stat-label">{icon} {title}</span>
      <span className="stat-value">{filesDone}<span className="subtle" style={{ fontSize: 14, fontWeight: 500 }}> / {files.length} files</span></span>
      <Progress value={pct(done, lines || 1)} tone={files.length && filesDone === files.length ? 'success' : undefined} />
      <span className="stat-sub">{done.toLocaleString()} / {lines.toLocaleString()} lines · {note}</span>
    </div>
  );
}

function LayerList({ d, track }: { d: LabProjectDetail; track: Track }) {
  const files = d.files.filter((f) => f.track === track);
  const byLayer = new Map<string, LabFileEntry[]>();
  files.forEach((f) => byLayer.set(f.layer, [...(byLayer.get(f.layer) ?? []), f]));
  const layers = d.layers.filter((l) => byLayer.has(l.layer));
  return (
    <div className="stack stagger">
      {layers.map((layer, i) => (
        <div key={layer.layer} className="card">
          <div className="row" style={{ gap: 12, flexWrap: 'nowrap', alignItems: 'flex-start' }}>
            <span className="step-num">{i + 1}</span>
            <div style={{ minWidth: 0 }}>
              <h3>{layer.label}</h3>
              <p className="small muted" style={{ marginTop: 2 }}>{layer.why}</p>
            </div>
          </div>
          <div style={{ marginTop: 10 }}>
            {byLayer.get(layer.layer)!.map((f) => (
              <Link key={f.id} to={`/lab/f/${f.id}`} className="file-row">
                {f.completed ? <CheckCircle2 size={16} color="var(--success)" /> : <span className="timeline-dot" style={{ width: 9, height: 9 }} />}
                <span className="path">{f.path}</span>
                <Progress value={pct(f.linesCompleted, f.lineCount || 1)} tone={f.completed ? 'success' : undefined} className="progress-thin" />
                <span className="tiny subtle" style={{ textAlign: 'right' }}>{f.linesCompleted}/{f.lineCount}</span>
              </Link>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}

function SyncCard({ projectId, projectName }: { projectId: string; projectName: string }) {
  const qc = useQueryClient();
  const toast = useToast();
  const input = useRef<HTMLInputElement>(null);
  const [downloading, setDownloading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [result, setResult] = useState<SyncResult | null>(null);

  const download = async () => {
    setDownloading(true);
    try {
      await downloadFile(`/api/lab/projects/${projectId}/progress/export`, `${projectName}-rebuild-progress.zip`);
      toast('Downloaded - unzip it and open the folder in your IDE');
    } catch (e) {
      toast((e as Error).message, 'error');
    } finally {
      setDownloading(false);
    }
  };

  const upload = async (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    setUploading(true);
    try {
      const form = new FormData();
      form.append('file', file);
      const r = await api<SyncResult>(`/api/lab/projects/${projectId}/progress/import`, { method: 'POST', form });
      setResult(r);
      qc.invalidateQueries({ queryKey: keys.labProject(projectId) });
      toast(r.linesAfter > r.linesBefore ? `Synced: +${r.linesAfter - r.linesBefore} lines` : 'Synced - nothing new to add');
    } catch (err) {
      toast((err as Error).message, 'error');
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="card sync-card" style={{ marginBottom: 22 }}>
      <div className="row" style={{ gap: 12, alignItems: 'flex-start', flexWrap: 'nowrap' }}>
        <Laptop size={22} style={{ flex: 'none', color: 'var(--accent)', marginTop: 2 }} />
        <div className="grow">
          <span className="card-title">Continue on your computer</span>
          <p className="small muted" style={{ marginTop: 4 }}>
            Download a zip with every file you started (cut at the line you reached) plus the originals in <code>_reference/</code>. Keep typing in IntelliJ / VS Code, then zip the folder and sync it back - matching lines become your progress here.
          </p>
          <div className="row" style={{ marginTop: 12 }}>
            <Button variant="outline" size="sm" loading={downloading} onClick={download}><Download size={14} /> Download progress</Button>
            <Button variant="outline" size="sm" loading={uploading} onClick={() => input.current?.click()}><Upload size={14} /> Sync from computer</Button>
            <input ref={input} type="file" accept=".zip,application/zip" hidden onChange={upload} />
          </div>
        </div>
      </div>
      {result && (
        <div className="sync-result">
          <div className="small" style={{ fontWeight: 600 }}>
            {result.filesMatched} file{result.filesMatched === 1 ? '' : 's'} matched · {result.filesAdvanced} advanced · {result.linesBefore} → {result.linesAfter} lines
          </div>
          {result.files.filter((f) => f.after > f.before || f.mismatchLine).map((f) => (
            <div key={f.path} className="sync-row">
              <span className="path mono">{f.path}</span>
              <span className="tiny">{f.before} → <strong>{f.after}</strong> / {f.lineCount}</span>
              {f.mismatchLine && (
                <span className="tiny sync-mismatch">
                  Your line {f.mismatchLine} differs. Expected <code>{f.expected}</code>, found <code>{f.found}</code>
                </span>
              )}
            </div>
          ))}
          {result.unmatched.length > 0 && <p className="tiny subtle">Not part of this project (ignored): {result.unmatched.slice(0, 5).join(', ')}{result.unmatched.length > 5 ? ` +${result.unmatched.length - 5} more` : ''}</p>}
        </div>
      )}
    </div>
  );
}
