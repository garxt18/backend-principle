import { useMutation } from '@tanstack/react-query';
import { Moon, Sun } from 'lucide-react';
import { useState, type FormEvent } from 'react';
import { Button, Chips, Field } from '../components/ui';
import { api } from '../lib/api';
import { useAuth } from '../lib/auth';
import { useTheme, type Theme } from '../lib/theme';
import { useToast } from '../lib/toast';
import type { Language, User } from '../lib/types';

export default function Settings() {
  const { user, setUser } = useAuth();
  const { theme, setTheme } = useTheme();
  const toast = useToast();
  const [name, setName] = useState(user!.displayName);
  const [language, setLanguage] = useState<Language>(user!.preferredLanguage);
  const [current, setCurrent] = useState('');
  const [next, setNext] = useState('');

  const profile = useMutation({
    mutationFn: () => api<User>('/api/me', { method: 'PATCH', body: { displayName: name, preferredLanguage: language } }),
    onSuccess: (u) => { setUser(u); toast('Profile saved'); },
    onError: (e) => toast(e.message, 'error'),
  });
  const password = useMutation({
    mutationFn: () => api('/api/me/password', { method: 'PUT', body: { currentPassword: current, newPassword: next } }),
    onSuccess: () => { setCurrent(''); setNext(''); toast('Password changed - other devices were logged out'); },
    onError: (e) => toast(e.message, 'error'),
  });

  return (
    <div className="container page" style={{ maxWidth: 760 }}>
      <div className="page-header">
        <div>
          <div className="eyebrow">Account</div>
          <h1>Settings</h1>
          <p>{user!.email}</p>
        </div>
      </div>
      <div className="stack-lg">
        <div className="card stack">
          <span className="card-title">Appearance</span>
          <Chips<Theme> label="Theme" value={theme} onChange={setTheme} options={[
            { value: 'dark', label: <span className="row" style={{ gap: 6 }}><Moon size={14} /> Dark</span> },
            { value: 'light', label: <span className="row" style={{ gap: 6 }}><Sun size={14} /> Light</span> },
          ]} />
        </div>
        <form className="card stack" onSubmit={(e: FormEvent) => { e.preventDefault(); profile.mutate(); }}>
          <span className="card-title">Profile</span>
          <Field label="Display name"><input className="input" value={name} minLength={2} maxLength={80} onChange={(e) => setName(e.target.value)} required /></Field>
          <div className="field">
            <span className="field-label">Default resource language</span>
            <Chips<Language> label="Resource language" value={language} onChange={setLanguage} options={[{ value: 'BOTH', label: 'Hindi + English' }, { value: 'HINDI', label: 'Hindi' }, { value: 'ENGLISH', label: 'English' }]} />
          </div>
          <div><Button type="submit" variant="primary" loading={profile.isPending}>Save profile</Button></div>
        </form>
        <form className="card stack" onSubmit={(e: FormEvent) => { e.preventDefault(); password.mutate(); }}>
          <span className="card-title">Change password</span>
          <div className="grid grid-2">
            <Field label="Current password"><input className="input" type="password" autoComplete="current-password" value={current} onChange={(e) => setCurrent(e.target.value)} required /></Field>
            <Field label="New password" hint="At least 8 characters"><input className="input" type="password" autoComplete="new-password" minLength={8} value={next} onChange={(e) => setNext(e.target.value)} required /></Field>
          </div>
          <div><Button type="submit" loading={password.isPending}>Change password</Button></div>
        </form>
      </div>
    </div>
  );
}
