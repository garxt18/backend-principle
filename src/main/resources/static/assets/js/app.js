import { api, onLogout, refreshSession, setAccessToken } from './api.js';
import { h, mount, toast } from './dom.js';
import { state } from './state.js';
import { renderAuth } from './views/auth.js';
import { renderDashboard } from './views/dashboard.js';
import { renderRoadmap } from './views/roadmap.js';
import { renderPlanner } from './views/planner.js';
import { renderLabHome, renderLabProject, renderLabFile } from './views/lab.js';
import { renderMentor } from './views/mentor.js';
import { renderSettings } from './views/settings.js';
import { renderAdmin } from './views/admin.js';

const app = document.getElementById('app');

const routes = [
    { pattern: /^#\/dashboard$/, view: renderDashboard, nav: 'dashboard' },
    { pattern: /^#\/roadmap$/, view: renderRoadmap, nav: 'roadmap' },
    { pattern: /^#\/planner$/, view: renderPlanner, nav: 'planner' },
    { pattern: /^#\/lab$/, view: renderLabHome, nav: 'lab' },
    { pattern: /^#\/lab\/p\/([\w-]+)$/, view: renderLabProject, nav: 'lab' },
    { pattern: /^#\/lab\/f\/([\w-]+)$/, view: renderLabFile, nav: 'lab' },
    { pattern: /^#\/mentor$/, view: renderMentor, nav: 'mentor' },
    { pattern: /^#\/settings$/, view: renderSettings, nav: 'settings' },
    { pattern: /^#\/admin$/, view: renderAdmin, nav: 'admin', admin: true },
];

const NAV = [
    ['dashboard', 'Dashboard'],
    ['roadmap', 'Roadmap'],
    ['planner', 'Planner'],
    ['lab', 'Rebuild Lab'],
    ['mentor', 'AI Mentor'],
    ['settings', 'Settings'],
];

let main;

function shell(active) {
    const links = NAV.map(([key, label]) => h('a', { href: `#/${key}`, class: key === active ? 'active' : '', text: label }));
    if (state.user?.role === 'ADMIN') {
        links.push(h('a', { href: '#/admin', class: active === 'admin' ? 'active' : '', text: 'Admin' }));
    }
    main = h('main', { class: 'main', id: 'main' });
    mount(app, h('div', { class: 'shell' },
        h('aside', { class: 'sidebar' },
            h('div', { class: 'brand' }, h('img', { src: '/favicon.svg', alt: '' }), 'Backend Playground'),
            h('nav', { class: 'nav', 'aria-label': 'Main' }, links),
            h('div', { class: 'spacer' }),
            h('div', { class: 'userbox' },
                h('div', { text: state.user.displayName }),
                h('button', { class: 'btn small', onclick: logout, text: 'Log out' }))),
        main));
    return main;
}

export async function logout() {
    try { await api('/api/auth/logout', { method: 'POST' }); } catch { /* already logged out */ }
    setAccessToken(null);
    state.user = null;
    state.progress = null;
    location.hash = '';
    render();
}

export function loggedIn(auth) {
    setAccessToken(auth.accessToken);
    state.user = auth.user;
    if (!location.hash || location.hash === '#/') location.hash = '#/dashboard';
    render();
}

async function render() {
    if (!state.user) {
        renderAuth(app);
        return;
    }
    const hash = location.hash || '#/dashboard';
    const route = routes.find(r => r.pattern.test(hash)) || routes[0];
    if (route.admin && state.user.role !== 'ADMIN') {
        location.hash = '#/dashboard';
        return;
    }
    const params = hash.match(route.pattern)?.slice(1) || [];
    const container = shell(route.nav);
    container.append(h('div', { class: 'boot', text: 'Loading...' }));
    try {
        await route.view(container, ...params);
    } catch (e) {
        mount(container, h('div', { class: 'error-box', text: e.message || 'Something went wrong' }));
        if (e.status !== 401) console.error(e);
    }
    container.focus?.();
}

onLogout(() => {
    state.user = null;
    toast('Your session expired - please log in again', 'error');
    render();
});

window.addEventListener('hashchange', render);

(async function boot() {
    const session = await refreshSession().catch(() => null);
    if (session) state.user = session.user;
    if (state.user && (!location.hash || location.hash === '#/')) location.hash = '#/dashboard';
    render();
})();
