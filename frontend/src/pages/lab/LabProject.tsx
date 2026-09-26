import { useQuery } from '@tanstack/react-query';
import { ArrowLeft, CheckCircle2, Play } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import { ErrorState, PageSkeleton, Progress } from '../../components/ui';
import { api } from '../../lib/api';
import { pct } from '../../lib/format';
import { keys } from '../../lib/queries';
import type { LabFileEntry, LabProjectDetail } from '../../lib/types';

export default function LabProject() {
  const { projectId = '' } = useParams();
  const detail = useQuery({ queryKey: keys.labProject(projectId), queryFn: () => api<LabProjectDetail>(`/api/lab/projects/${projectId}`) });
  if (detail.isPending) return <div className="container"><PageSkeleton /></div>;
  if (detail.error) return <div className="container page"><ErrorState error={detail.error} /></div>;
  const d = detail.data!;
  const next = d.files.find((f) => !f.completed) ?? d.files[0];
  const byLayer = new Map<string, LabFileEntry[]>();
  d.files.forEach((f) => byLayer.set(f.layer, [...(byLayer.get(f.layer) ?? []), f]));

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

      <div className="card" style={{ marginBottom: 16 }}>
        <div className="row-between small"><strong>{d.percent}% rebuilt</strong><span className="muted">{d.linesCompleted.toLocaleString()} / {d.project.totalLines.toLocaleString()} lines</span></div>
        <Progress value={d.percent} />
      </div>

      <div className="stack stagger">
        {d.layers.map((layer, i) => (
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
    </div>
  );
}
