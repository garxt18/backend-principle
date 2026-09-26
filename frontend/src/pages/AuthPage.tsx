import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Button, Chips, Field } from '../components/ui';
import { api, ApiError } from '../lib/api';
import { useAuth } from '../lib/auth';
import type { AuthResponse, Language } from '../lib/types';

export default function AuthPage({ mode }: { mode: 'login' | 'signup' }) {
  const { signIn } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [language, setLanguage] = useState<Language>('BOTH');
  const [error, setError] = useState<ApiError | null>(null);
  const [busy, setBusy] = useState(false);
  const isSignup = mode === 'signup';

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const body = isSignup ? { email, password, displayName: name, preferredLanguage: language } : { email, password };
      const auth = await api<AuthResponse>(isSignup ? '/api/auth/register' : '/api/auth/login', { method: 'POST', body });
      signIn(auth);
      const from = (location.state as { from?: string } | null)?.from;
      navigate(from && from !== '/login' ? from : '/dashboard', { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err : new ApiError(0, { detail: 'Network error - is the server running?' }));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth-wrap">
      <form className="card auth-card stack-lg" onSubmit={submit} noValidate={false}>
        <div>
          <h1>{isSignup ? 'Create your account' : 'Welcome back'}</h1>
          <p className="muted">{isSignup ? 'Free, takes 20 seconds. Your progress is private to you.' : 'Log in to continue where you left off.'}</p>
        </div>
        {error && <div className="alert alert-danger" role="alert">{error.message}</div>}
        {isSignup && (
          <Field label="Your name" error={error?.fieldErrors.displayName}>
            <input className="input" value={name} onChange={(e) => setName(e.target.value)} required minLength={2} maxLength={80} autoComplete="name" autoFocus />
          </Field>
        )}
        <Field label="Email" error={error?.fieldErrors.email}>
          <input className="input" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" autoFocus={!isSignup} />
        </Field>
        <Field label="Password" hint={isSignup ? 'At least 8 characters' : undefined} error={error?.fieldErrors.password}>
          <input className="input" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={isSignup ? 8 : undefined} autoComplete={isSignup ? 'new-password' : 'current-password'} />
        </Field>
        {isSignup && (
          <div className="field">
            <span className="field-label">Show me resources in</span>
            <Chips<Language>
              label="Resource language"
              value={language}
              onChange={setLanguage}
              options={[{ value: 'BOTH', label: 'Hindi + English' }, { value: 'HINDI', label: 'Hindi' }, { value: 'ENGLISH', label: 'English' }]}
            />
          </div>
        )}
        <Button type="submit" variant="primary" size="lg" loading={busy}>{isSignup ? 'Create account' : 'Log in'}</Button>
        <p className="small muted" style={{ textAlign: 'center' }}>
          {isSignup ? <>Already have an account? <Link className="link" to="/login">Log in</Link></> : <>New here? <Link className="link" to="/signup">Create an account</Link></>}
        </p>
      </form>
    </div>
  );
}
