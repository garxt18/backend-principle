import { api } from '../api.js';
import { field, formatDate, h, mount, select, toast } from '../dom.js';
import { loadRoadmap, state } from '../state.js';

export async function renderAdmin(root) {
    const [stats, levels] = await Promise.all([api('/api/admin/stats'), loadRoadmap(true)]);
    const usersBox = h('div');
    const search = h('input', { type: 'text', placeholder: 'Search email or name' });
    let timer;
    search.addEventListener('input', () => { clearTimeout(timer); timer = setTimeout(() => loadUsers(search.value), 300); });

    async function loadUsers(q = '') {
        const page = await api(`/api/admin/users?q=${encodeURIComponent(q)}&size=50`);
        mount(usersBox, h('div', { class: 'table-wrap' }, h('table', {},
            h('thead', {}, h('tr', {}, ['Name', 'Email', 'Role', 'Joined', 'Status'].map(t => h('th', { text: t })))),
            h('tbody', {}, page.items.map(u => {
                const self = u.id === state.user.id;
                const role = select([['USER', 'USER'], ['ADMIN', 'ADMIN']], u.role, { disabled: self, onchange: e => patch(u.id, { role: e.target.value }) });
                const enabled = h('input', { type: 'checkbox', checked: u.enabled, disabled: self, 'aria-label': 'Enabled', onchange: e => patch(u.id, { enabled: e.target.checked }) });
                return h('tr', {}, h('td', { text: u.displayName }), h('td', { text: u.email }), h('td', {}, role),
                    h('td', { text: formatDate(u.createdAt) }), h('td', {}, h('label', { class: 'row small' }, enabled, 'enabled')));
            })))),
        h('p', { class: 'muted small', text: `${page.totalItems} users` }));
    }

    async function patch(id, body) {
        try { await api(`/api/admin/users/${id}`, { method: 'PATCH', body }); toast('User updated'); }
        catch (e) { toast(e.message, 'error'); }
    }

    const levelSelect = select(levels.map(l => [l.id, `L${l.levelNumber} - ${l.title}`]), levels[0]?.id);
    const resBox = h('div');
    levelSelect.addEventListener('change', drawResources);

    function drawResources() {
        const level = state.roadmap.find(l => String(l.id) === levelSelect.value);
        const title = h('input', { type: 'text', required: true, maxlength: 200 });
        const url = h('input', { type: 'text', required: true, placeholder: 'https://www.youtube.com/...', maxlength: 1000 });
        const channel = h('input', { type: 'text', maxlength: 120 });
        const language = select([['HI', 'Hindi'], ['EN', 'English']], 'HI');
        const kind = select(['PLAYLIST', 'VIDEO', 'CHANNEL', 'COURSE', 'DOCS', 'SEARCH'].map(k => [k, k]), 'PLAYLIST');
        const primary = h('input', { type: 'checkbox' });
        mount(resBox,
            level.resources.map(r => h('div', { class: 'resource' },
                h('span', { class: `badge ${r.language === 'HI' ? 'hi' : 'en'}`, text: r.language }),
                h('a', { href: r.url, target: '_blank', rel: 'noopener noreferrer', text: r.title }),
                h('span', { class: 'muted small', text: r.channel || '' }),
                h('button', { class: 'btn small danger right', text: 'Delete', onclick: async () => {
                    if (!confirm(`Delete "${r.title}"?`)) return;
                    try { await api(`/api/admin/resources/${r.id}`, { method: 'DELETE' }); await loadRoadmap(true); drawResources(); }
                    catch (e) { toast(e.message, 'error'); }
                } }))),
            h('form', { class: 'stack', style: { marginTop: '1rem' }, onsubmit: async e => {
                e.preventDefault();
                try {
                    await api('/api/admin/resources', { method: 'POST', body: { levelId: level.id, title: title.value, url: url.value,
                        channel: channel.value || null, language: language.value, kind: kind.value, primaryPick: primary.checked,
                        orderIndex: level.resources.length } });
                    await loadRoadmap(true);
                    drawResources();
                    toast('Resource added');
                } catch (err) { toast(err.message + ' ' + Object.values(err.fieldErrors).join(', '), 'error'); }
            } },
                h('h3', { text: 'Add resource' }),
                h('div', { class: 'grid cols-2' }, field('Title', title), field('URL (https)', url), field('Channel', channel),
                    h('div', { class: 'row' }, field('Language', language), field('Kind', kind), h('label', { class: 'row small' }, primary, 'Start here'))),
                h('div', {}, h('button', { class: 'btn primary', type: 'submit', text: 'Add' }))));
    }

    mount(root,
        h('div', { class: 'page-head' }, h('div', {}, h('h1', { text: 'Admin' }), h('p', { text: 'Manage learners and curated resources.' }))),
        h('div', { class: 'grid cols-4' },
            h('div', { class: 'card stat' }, h('div', { class: 'label', text: 'Users' }), h('div', { class: 'value', text: stats.users })),
            h('div', { class: 'card stat' }, h('div', { class: 'label', text: 'Active last 7 days' }), h('div', { class: 'value', text: stats.activeLearnersLast7Days })),
            h('div', { class: 'card stat' }, h('div', { class: 'label', text: 'Lab projects' }), h('div', { class: 'value', text: stats.labProjects }))),
        h('div', { class: 'card', style: { marginTop: '1rem' } }, h('div', { class: 'row' }, h('h2', { text: 'Users' }), h('span', { class: 'right' }, search)), usersBox),
        h('div', { class: 'card', style: { marginTop: '1rem' } }, h('div', { class: 'row' }, h('h2', { text: 'Resources' }), h('span', { class: 'right' }, levelSelect)), resBox));
    await loadUsers();
    drawResources();
}
