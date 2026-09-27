import { clsx } from 'clsx';
import {
  Code2,
  CalendarCheck2,
  Hammer,
  LayoutDashboard,
  LogOut,
  Map,
  Menu,
  Moon,
  Settings,
  Shield,
  Sun,
  X,
} from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../lib/auth';
import { initials } from '../../lib/format';
import { useTheme } from '../../lib/theme';
import { Button } from '../ui';

const LINKS = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/roadmap', label: 'Roadmap', icon: Map },
  { to: '/planly', label: 'Planly', icon: CalendarCheck2 },
  { to: '/lab', label: 'Rebuild Lab', icon: Hammer },
];

export function Brand() {
  return (
    <Link to="/" className="brand" aria-label="Backend Playground home">
      <span className="brand-mark"><Code2 size={17} strokeWidth={2.6} /></span>
      <span>Backend<span className="dim">Playground</span></span>
    </Link>
  );
}

export function ThemeToggle() {
  const { theme, toggle } = useTheme();
  return (
    <Button variant="ghost" icon onClick={toggle} aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} theme`} title="Toggle theme">
      {theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
    </Button>
  );
}

export function Navbar() {
  const { user, status, signOut } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const profileRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setMenuOpen(false);
    setProfileOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    if (!profileOpen) return;
    const close = (e: MouseEvent) => !profileRef.current?.contains(e.target as Node) && setProfileOpen(false);
    const esc = (e: KeyboardEvent) => e.key === 'Escape' && setProfileOpen(false);
    document.addEventListener('mousedown', close);
    document.addEventListener('keydown', esc);
    return () => {
      document.removeEventListener('mousedown', close);
      document.removeEventListener('keydown', esc);
    };
  }, [profileOpen]);

  const authed = status === 'authenticated' && user;
  const links = authed ? LINKS : [];

  return (
    <>
      <header className="navbar">
        <div className="container">
          <Brand />
          <nav className="nav-links" aria-label="Main">
            {links.map((l) => (
              <NavLink key={l.to} to={l.to} className={({ isActive }) => clsx('nav-link', isActive && 'active')}>
                {l.label}
              </NavLink>
            ))}
          </nav>
          <div className="nav-right">
            <ThemeToggle />
            {authed ? (
              <>
                <div className="dropdown" ref={profileRef}>
                  <button className="avatar" aria-haspopup="menu" aria-expanded={profileOpen} aria-label="Account menu" onClick={() => setProfileOpen((o) => !o)}>
                    {initials(user.displayName)}
                  </button>
                  {profileOpen && (
                    <div className="dropdown-menu" role="menu">
                      <div className="dropdown-head">
                        <div style={{ fontWeight: 600 }}>{user.displayName}</div>
                        <div className="small subtle truncate">{user.email}</div>
                      </div>
                      <button className="dropdown-item" role="menuitem" onClick={() => navigate('/settings')}><Settings size={16} /> Settings</button>
                      {user.role === 'ADMIN' && (
                        <button className="dropdown-item" role="menuitem" onClick={() => navigate('/admin')}><Shield size={16} /> Admin</button>
                      )}
                      <button className="dropdown-item" role="menuitem" onClick={async () => { await signOut(); navigate('/'); }}><LogOut size={16} /> Log out</button>
                    </div>
                  )}
                </div>
                <Button variant="ghost" icon className="nav-burger" aria-label="Open menu" aria-expanded={menuOpen} onClick={() => setMenuOpen((o) => !o)}>
                  {menuOpen ? <X size={20} /> : <Menu size={20} />}
                </Button>
              </>
            ) : status === 'anonymous' ? (
              <>
                <Link to="/login" className="btn btn-ghost btn-sm">Log in</Link>
                <Link to="/signup" className="btn btn-primary btn-sm">Sign up free</Link>
              </>
            ) : null}
          </div>
        </div>
      </header>
      {menuOpen && authed && (
        <nav className="mobile-menu" aria-label="Mobile">
          {LINKS.map((l) => (
            <NavLink key={l.to} to={l.to} className={({ isActive }) => clsx('nav-link', isActive && 'active')}>
              <l.icon size={18} /> {l.label}
            </NavLink>
          ))}
          <NavLink to="/settings" className={({ isActive }) => clsx('nav-link', isActive && 'active')}><Settings size={18} /> Settings</NavLink>
        </nav>
      )}
    </>
  );
}
