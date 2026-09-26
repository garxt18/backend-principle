import { lazy, Suspense, useEffect } from 'react';
import { Route, Routes, useLocation } from 'react-router-dom';
import { ErrorBoundary, GuestOnly, RequireAuth } from './components/layout/Guards';
import { Navbar } from './components/layout/Navbar';
import { PageSkeleton } from './components/ui';

// Each page is its own chunk: the landing page does not download the Rebuild Lab editor.
const Landing = lazy(() => import('./pages/Landing'));
const AuthPage = lazy(() => import('./pages/AuthPage'));
const Dashboard = lazy(() => import('./pages/Dashboard'));
const Roadmap = lazy(() => import('./pages/Roadmap'));
const DsaSheet = lazy(() => import('./pages/DsaSheet'));
const Planly = lazy(() => import('./pages/Planly'));
const LabHome = lazy(() => import('./pages/lab/LabHome'));
const LabProject = lazy(() => import('./pages/lab/LabProject'));
const LabEditor = lazy(() => import('./pages/lab/LabEditor'));
const Settings = lazy(() => import('./pages/Settings'));
const Admin = lazy(() => import('./pages/Admin'));
const NotFound = lazy(() => import('./pages/NotFound'));

const TITLES: [RegExp, string][] = [
  [/^\/dashboard/, 'Dashboard'],
  [/^\/roadmap/, 'Roadmap'],
  [/^\/dsa/, 'DSA Sheet'],
  [/^\/planly/, 'Planly'],
  [/^\/lab/, 'Rebuild Lab'],
  [/^\/settings/, 'Settings'],
  [/^\/admin/, 'Admin'],
  [/^\/login/, 'Log in'],
  [/^\/signup/, 'Sign up'],
];

function useRouteEffects() {
  const { pathname } = useLocation();
  useEffect(() => {
    window.scrollTo({ top: 0 });
    const title = TITLES.find(([re]) => re.test(pathname))?.[1];
    document.title = title ? `${title} · Backend Playground` : 'Backend Playground';
  }, [pathname]);
}

export function App() {
  useRouteEffects();
  return (
    <>
      <Navbar />
      <ErrorBoundary>
        <Suspense fallback={<div className="container"><PageSkeleton /></div>}>
          <Routes>
            <Route path="/" element={<GuestOnly><Landing /></GuestOnly>} />
            <Route path="/login" element={<GuestOnly><AuthPage mode="login" /></GuestOnly>} />
            <Route path="/signup" element={<GuestOnly><AuthPage mode="signup" /></GuestOnly>} />
            <Route element={<RequireAuth />}>
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/roadmap" element={<Roadmap />} />
              <Route path="/dsa" element={<DsaSheet />} />
              <Route path="/planly" element={<Planly />} />
              <Route path="/lab" element={<LabHome />} />
              <Route path="/lab/p/:projectId" element={<LabProject />} />
              <Route path="/lab/f/:fileId" element={<LabEditor />} />
              <Route path="/settings" element={<Settings />} />
            </Route>
            <Route element={<RequireAuth admin />}>
              <Route path="/admin" element={<Admin />} />
            </Route>
            <Route path="*" element={<NotFound />} />
          </Routes>
        </Suspense>
      </ErrorBoundary>
    </>
  );
}
