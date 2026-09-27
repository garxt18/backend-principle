import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FileArchive, FolderGit2, Trash2, UploadCloud } from 'lucide-react';
import { useRef, useState, type DragEvent, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Button, ConfirmDialog, ErrorState, Field, PageSkeleton } from '../../components/ui';
import { api } from '../../lib/api';
import { formatDate } from '../../lib/format';
import { keys } from '../../lib/queries';
import { useToast } from '../../lib/toast';
import type { LabProject } from '../../lib/types';

export default function LabHome() {
  const projects = useQuery({ queryKey: keys.labProjects, queryFn: () => api<LabProject[]>('/api/lab/projects') });
  if (projects.isPending) return <div className="container"><PageSkeleton /></div>;
  if (projects.error) return <div className="container page"><ErrorState error={projects.error} /></div>;
  const templates = projects.data!.filter((p) => p.template);
  const mine = projects.data!.filter((p) => !p.template);

  return (
    <div className="container page">
      <div className="page-header">
        <div>
          <div className="eyebrow">Rebuild Lab</div>
          <h1>Rebuild real projects, line by line</h1>
          <p>Upload a Spring Boot or Next.js project. The Lab orders the files the way a senior engineer builds them and explains every line - what it does and why - while you retype it.</p>
          <p className="small muted" style={{ marginTop: 6 }}>
            <strong>Spring Boot:</strong> pom.xml → config → schema → entities → repositories → DTOs → services → controllers → tests → Docker<br />
            <strong>Next.js:</strong> package.json → .env → Prisma schema → types & zod → data access → server actions → proxy → route handlers → components → pages
          </p>
        </div>
      </div>
      <div className="grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(min(420px, 100%), 1fr))', alignItems: 'start' }}>
        <div className="stack">
          <h2 style={{ fontSize: '1rem' }}>Your projects</h2>
          {mine.length === 0 ? (
            <div className="card muted small">Nothing uploaded yet. Upload your own project on the right - only you can see it.</div>
          ) : (
            <div className="stack stagger">{mine.map((p) => <ProjectCard key={p.id} p={p} />)}</div>
          )}
          <h2 style={{ fontSize: '1rem', marginTop: 10 }}>Starter templates</h2>
          <div className="stack stagger">{templates.map((p) => <ProjectCard key={p.id} p={p} />)}</div>
        </div>
        <Upload />
      </div>
    </div>
  );
}

function ProjectCard({ p }: { p: LabProject }) {
  const qc = useQueryClient();
  const toast = useToast();
  const [confirm, setConfirm] = useState(false);
  const del = useMutation({
    mutationFn: () => api(`/api/lab/projects/${p.id}`, { method: 'DELETE' }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: keys.labProjects });
      toast('Project deleted');
    },
    onError: (e) => toast(e.message, 'error'),
  });
  return (
    <div className="card card-hover">
      <div className="row-between">
        <Link to={`/lab/p/${p.id}`} className="row" style={{ flexWrap: 'nowrap', minWidth: 0 }}>
          <span className="feature-icon" style={{ width: 34, height: 34, flex: 'none' }}><FolderGit2 size={18} /></span>
          <span style={{ minWidth: 0 }}>
            <span className="row" style={{ gap: 8, flexWrap: 'nowrap', minWidth: 0 }}>
              <span className="card-title truncate">{p.name}</span>
              <span className={p.stack === 'NEXTJS' ? 'badge' : 'badge badge-accent'} style={{ flex: 'none' }}>{p.stackLabel}</span>
            </span>
            <span className="tiny subtle">{p.fileCount} files · {p.totalLines.toLocaleString()} lines{p.template ? ' · template' : ` · uploaded ${formatDate(p.createdAt)}`}</span>
          </span>
        </Link>
        <div className="row">
          {!p.template && <Button size="sm" variant="ghost" icon aria-label={`Delete ${p.name}`} onClick={() => setConfirm(true)}><Trash2 size={15} /></Button>}
          <Link to={`/lab/p/${p.id}`} className="btn btn-sm btn-primary">Open</Link>
        </div>
      </div>
      {p.description && <p className="small muted" style={{ marginTop: 10 }}>{p.description}</p>}
      {confirm && (
        <ConfirmDialog title={`Delete "${p.name}"?`} body="The project and your rebuild progress on it are deleted permanently." confirmLabel="Delete" danger
          onClose={() => setConfirm(false)} onConfirm={() => { setConfirm(false); del.mutate(); }} />
      )}
    </div>
  );
}

function Upload() {
  const qc = useQueryClient();
  const toast = useToast();
  const navigate = useNavigate();
  const input = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [drag, setDrag] = useState(false);
  const [skipped, setSkipped] = useState<string[]>([]);

  const upload = useMutation({
    mutationFn: () => {
      const form = new FormData();
      form.append('file', file!);
      form.append('name', name);
      if (description) form.append('description', description);
      return api<{ project: LabProject; skipped: string[] }>('/api/lab/projects', { method: 'POST', form });
    },
    onSuccess: (res) => {
      qc.invalidateQueries({ queryKey: keys.labProjects });
      toast(`Imported ${res.project.fileCount} files`);
      if (res.skipped.length) setSkipped(res.skipped);
      navigate(`/lab/p/${res.project.id}`);
    },
    onError: (e) => toast(e.message, 'error'),
  });

  const pick = (f: File | undefined) => {
    if (!f) return;
    if (!f.name.toLowerCase().endsWith('.zip')) {
      toast('Please choose a .zip file', 'error');
      return;
    }
    if (f.size > 5 * 1024 * 1024) {
      toast('The zip is larger than 5 MB - remove target/, build/, .next/ and node_modules/ first', 'error');
      return;
    }
    setFile(f);
    if (!name) setName(f.name.replace(/\.zip$/i, ''));
  };
  const onDrop = (e: DragEvent) => {
    e.preventDefault();
    setDrag(false);
    pick(e.dataTransfer.files[0]);
  };
  const submit = (e: FormEvent) => {
    e.preventDefault();
    if (file) upload.mutate();
  };

  return (
    <form className="card stack" onSubmit={submit} style={{ position: 'sticky', top: 'calc(var(--nav-h) + 16px)' }}>
      <h2 style={{ fontSize: '1rem' }}>Upload a project</h2>
      <div
        className={`dropzone ${drag ? 'drag' : ''}`}
        role="button"
        tabIndex={0}
        onClick={() => input.current?.click()}
        onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && input.current?.click()}
        onDragOver={(e) => { e.preventDefault(); setDrag(true); }}
        onDragLeave={() => setDrag(false)}
        onDrop={onDrop}
      >
        {file ? <FileArchive size={30} /> : <UploadCloud size={30} />}
        <div style={{ fontWeight: 600, color: 'var(--text)' }}>{file ? file.name : 'Drop your project .zip here'}</div>
        <div className="small">{file ? `${(file.size / 1024).toFixed(0)} KB · click to change` : 'or click to browse · max 5 MB'}</div>
        <input ref={input} type="file" accept=".zip,application/zip" hidden onChange={(e) => pick(e.target.files?.[0])} />
      </div>
      <Field label="Project name"><input className="input" value={name} maxLength={120} onChange={(e) => setName(e.target.value)} required placeholder="e.g. My E-Commerce API" /></Field>
      <Field label="Description (optional)"><textarea className="textarea" rows={2} maxLength={1000} value={description} onChange={(e) => setDescription(e.target.value)} placeholder="What does it do?" /></Field>
      {skipped.length > 0 && <div className="alert alert-info small">Skipped: {skipped.join(', ')}</div>}
      <Button type="submit" variant="primary" disabled={!file} loading={upload.isPending}>Upload & analyse</Button>
      <p className="tiny subtle">
        Tip: on GitHub use Code → Download ZIP. Java, Kotlin, XML, YAML, properties, SQL, Prisma, Dockerfile, JS/TS/TSX, CSS and Markdown files are imported;
        target/, build/, .git/, node_modules/, .next/ and lockfiles are skipped automatically. Next.js projects are detected from next.config or package.json.
      </p>
    </form>
  );
}
