import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Search, Trash2 } from 'lucide-react';
import { useDeferredValue, useState, type FormEvent } from 'react';
import { Button, ErrorState, Field, LangBadge, PageSkeleton } from '../components/ui';
import { api } from '../lib/api';
import { useAuth } from '../lib/auth';
import { formatDate } from '../lib/format';
import { keys, useRoadmap } from '../lib/queries';
import { useToast } from '../lib/toast';
import type { AdminStats, Page, ResourceKind, ResourceLang, Role, User } from '../lib/types';

export default function Admin() {
  const stats = useQuery({ queryKey: ['admin', 'stats'], queryFn: () => api<AdminStats>('/api/admin/stats') });
  if (stats.isPending) return <div className="container"><PageSkeleton /></div>;
  if (stats.error) return <div className="container page"><ErrorState error={stats.error} /></div>;
  const s = stats.data!;
  return (
    <div className="container page">
      <div className="page-header">
        <div>
          <div className="eyebrow">Admin</div>
          <h1>Admin</h1>
          <p>Manage learners and the curated resources everyone sees.</p>
        </div>
      </div>
      <div className="grid grid-3 stagger">
        <div className="card stat-card"><span className="stat-label">Users</span><span className="stat-value">{s.users}</span></div>
        <div className="card stat-card"><span className="stat-label">Active last 7 days</span><span className="stat-value">{s.activeLearnersLast7Days}</span></div>
        <div className="card stat-card"><span className="stat-label">Lab projects</span><span className="stat-value">{s.labProjects}</span></div>
      </div>
      <Users />
      <Resources />
    </div>
  );
}

function Users() {
  const { user: me } = useAuth();
  const toast = useToast();
  const qc = useQueryClient();
  const [q, setQ] = useState('');
  const query = useDeferredValue(q);
  const users = useQuery({ queryKey: ['admin', 'users', query], queryFn: () => api<Page<User>>(`/api/admin/users?size=50&q=${encodeURIComponent(query)}`) });
  const patch = useMutation({
    mutationFn: (v: { id: string; enabled?: boolean; role?: Role }) => api(`/api/admin/users/${v.id}`, { method: 'PATCH', body: v }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['admin', 'users'] }); toast('User updated'); },
    onError: (e) => toast(e.message, 'error'),
  });
  return (
    <div className="card" style={{ marginTop: 16 }}>
      <div className="card-head">
        <span className="card-title">Users</span>
        <div className="input-with-icon" style={{ width: 'min(260px, 100%)' }}><Search size={15} /><input className="input" placeholder="Search" value={q} onChange={(e) => setQ(e.target.value)} aria-label="Search users" /></div>
      </div>
      <div className="table-scroll">
        <table className="sheet">
          <thead><tr><th>Name</th><th>Email</th><th>Role</th><th className="hide-sm">Joined</th><th>Enabled</th></tr></thead>
          <tbody>
            {users.data?.items.map((u) => (
              <tr key={u.id}>
                <td>{u.displayName}</td>
                <td className="small muted">{u.email}</td>
                <td>
                  <select className="select status-pill" value={u.role} disabled={u.id === me?.id} onChange={(e) => patch.mutate({ id: u.id, role: e.target.value as Role })} aria-label="Role">
                    <option value="USER">User</option><option value="ADMIN">Admin</option>
                  </select>
                </td>
                <td className="small subtle hide-sm">{formatDate(u.createdAt, { year: 'numeric' })}</td>
                <td><input type="checkbox" checked={u.enabled} disabled={u.id === me?.id} onChange={(e) => patch.mutate({ id: u.id, enabled: e.target.checked })} aria-label="Enabled" /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {users.data && <p className="tiny subtle" style={{ marginTop: 10 }}>{users.data.totalItems} users</p>}
    </div>
  );
}

function Resources() {
  const roadmap = useRoadmap();
  const qc = useQueryClient();
  const toast = useToast();
  const [levelId, setLevelId] = useState<number | null>(null);
  const [form, setForm] = useState({ title: '', url: '', channel: '', language: 'HI' as ResourceLang, kind: 'PLAYLIST' as ResourceKind, primaryPick: false });
  const levels = roadmap.data ?? [];
  const level = levels.find((l) => l.id === levelId) ?? levels[0];
  const refresh = () => qc.invalidateQueries({ queryKey: keys.roadmap });
  const add = useMutation({
    mutationFn: () => api('/api/admin/resources', { method: 'POST', body: { ...form, channel: form.channel || null, levelId: level!.id, orderIndex: level!.resources.length } }),
    onSuccess: () => { refresh(); setForm((f) => ({ ...f, title: '', url: '', channel: '' })); toast('Resource added'); },
    onError: (e) => toast(e.message, 'error'),
  });
  const remove = useMutation({
    mutationFn: (id: number) => api(`/api/admin/resources/${id}`, { method: 'DELETE' }),
    onSuccess: () => { refresh(); toast('Resource deleted'); },
  });
  if (!level) return null;
  return (
    <div className="card" style={{ marginTop: 16 }}>
      <div className="card-head">
        <span className="card-title">Roadmap resources</span>
        <select className="select" style={{ width: 'min(320px, 100%)' }} value={level.id} onChange={(e) => setLevelId(Number(e.target.value))} aria-label="Level">
          {levels.map((l) => <option key={l.id} value={l.id}>L{l.levelNumber} · {l.title}</option>)}
        </select>
      </div>
      <div className="resource-list">
        {level.resources.map((r) => (
          <div key={r.id} className="resource">
            <span className="grow"><a href={r.url} target="_blank" rel="noopener noreferrer" className="r-title">{r.title}</a><span className="r-meta" style={{ display: 'block' }}>{r.kind} · {r.channel ?? '—'}</span></span>
            <LangBadge lang={r.language} />
            <Button size="sm" variant="ghost" icon aria-label={`Delete ${r.title}`} onClick={() => remove.mutate(r.id)}><Trash2 size={14} /></Button>
          </div>
        ))}
      </div>
      <form className="stack" style={{ marginTop: 16 }} onSubmit={(e: FormEvent) => { e.preventDefault(); add.mutate(); }}>
        <div className="grid grid-2">
          <Field label="Title"><input className="input" required maxLength={200} value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></Field>
          <Field label="URL (https)"><input className="input" required pattern="https://.+" value={form.url} onChange={(e) => setForm({ ...form, url: e.target.value })} /></Field>
          <Field label="Channel"><input className="input" maxLength={120} value={form.channel} onChange={(e) => setForm({ ...form, channel: e.target.value })} /></Field>
          <div className="grid" style={{ gridTemplateColumns: '1fr 1fr' }}>
            <Field label="Language"><select className="select" value={form.language} onChange={(e) => setForm({ ...form, language: e.target.value as ResourceLang })}><option value="HI">Hindi</option><option value="EN">English</option></select></Field>
            <Field label="Kind"><select className="select" value={form.kind} onChange={(e) => setForm({ ...form, kind: e.target.value as ResourceKind })}>{['PLAYLIST', 'VIDEO', 'CHANNEL', 'COURSE', 'DOCS', 'SEARCH'].map((k) => <option key={k}>{k}</option>)}</select></Field>
          </div>
        </div>
        <label className="row small"><input type="checkbox" checked={form.primaryPick} onChange={(e) => setForm({ ...form, primaryPick: e.target.checked })} /> Recommended starting point</label>
        <div><Button type="submit" variant="primary" loading={add.isPending}>Add resource</Button></div>
      </form>
    </div>
  );
}
