import { Component, type ErrorInfo, type ReactNode } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../../lib/auth';
import { PageSkeleton } from '../ui';

/** Pages behind login. While the session is being restored we show a skeleton instead of flashing the login page. */
export function RequireAuth({ admin }: { admin?: boolean }) {
  const { status, user } = useAuth();
  const location = useLocation();
  if (status === 'loading') return <div className="container"><PageSkeleton /></div>;
  if (status === 'anonymous') return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (admin && user?.role !== 'ADMIN') return <Navigate to="/dashboard" replace />;
  return <Outlet />;
}

/** Login/signup/landing: logged-in users go straight to their dashboard. */
export function GuestOnly({ children }: { children: ReactNode }) {
  const { status } = useAuth();
  if (status === 'loading') return null;
  if (status === 'authenticated') return <Navigate to="/dashboard" replace />;
  return <>{children}</>;
}

export class ErrorBoundary extends Component<{ children: ReactNode }, { error: Error | null }> {
  state = { error: null as Error | null };

  static getDerivedStateFromError(error: Error) {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('UI crashed', error, info.componentStack);
  }

  render() {
    if (this.state.error) {
      return (
        <div className="container page">
          <div className="card stack">
            <h2>Something broke on this page</h2>
            <p className="muted">{this.state.error.message}</p>
            <div><button className="btn btn-primary" onClick={() => location.reload()}>Reload</button></div>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}
