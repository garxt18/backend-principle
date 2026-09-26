import { api } from '../api.js';
import { field, formatDate, h, mount, progressBar, select, toast, today } from '../dom.js';
import { allTopics, loadProgress, loadRoadmap } from '../state.js';
import { planItem } from './dashboard.js';

export async function renderPlanner(root) {
    const [levels] = await Promise.all([loadRoadmap(), loadProgress()]);
    const [plan, sessions] = await Promise.all([
        api('/api/plans/current').catch(e => (e.status === 404 ? null : Promise.reject(e))),
        api('/api/sessions'),
    ]);

    const planBox = h('div');
    const showForm = () => mount(planBox, planForm(levels, plan, created => { mount(planBox, planView(created, showForm)); }));
    if (plan) mount(planBox, planView(plan, showForm));
    else showForm();

    mount(root,
        h('div', { class: 'page-head' },
            h('div', {}, h('h1', { text: 'Study Planner' }),
                h('p', { text: 'A week-by-week schedule generated from the roadmap and your available hours.' }))),
        planBox,
        h('div', { class: 'card', style: { marginTop: '1rem' } }, h('h2', { text: 'Recent study sessions' }), sessionsTable(sessions)));
}

function planForm(levels, existing, onCreated) {
    const start = h('input', { type: 'date', value: today(), required: true });
    const hours = h('input', { type: 'number', min: 1, max: 80, value: existing?.hoursPerWeek || 20, required: true });
    const levelOptions = levels.map(l => [l.levelNumber, `L${l.levelNumber} - ${l.title}`]);
    const from = select(levelOptions, 0);
    const to = select(levelOptions, levels.at(-1)?.levelNumber ?? 16);
    const skip = h('input', { type: 'checkbox', checked: true });
    const estimate = h('p', { class: 'muted small' });
    const totalHours = () => levels.filter(l => l.levelNumber >= Number(from.value) && l.levelNumber <= Number(to.value))
        .reduce((sum, l) => sum + l.totalHours, 0);
    const update = () => {
        const weeks = Math.ceil(totalHours() / Math.max(1, Number(hours.value)));
        estimate.textContent = `About ${totalHours()} hours -> roughly ${weeks} weeks (${Math.round(weeks / 4.3)} months) at ${hours.value} h/week. `
            + 'The roadmap targets ~6 months, which needs about 20 h/week.';
    };
    [hours, from, to].forEach(el => el.addEventListener('input', update));
    update();

    return h('form', { class: 'card stack', onsubmit: async e => {
        e.preventDefault();
        try {
            const plan = await api('/api/plans', { method: 'POST', body: {
                startDate: start.value, hoursPerWeek: Number(hours.value),
                fromLevel: Number(from.value), toLevel: Number(to.value), skipCompleted: skip.checked } });
            toast(`Plan created: ${plan.totalWeeks} weeks`);
            onCreated(plan);
        } catch (err) { toast(err.message, 'error'); }
    } },
        h('h2', { text: existing ? 'Re-plan' : 'Create your plan' }),
        existing ? h('p', { class: 'note-box small', text: 'Creating a new plan archives the current one. Your topic progress is kept.' }) : null,
        h('div', { class: 'grid cols-4' }, field('Start date', start), field('Hours per week', hours), field('From level', from), field('To level', to)),
        h('label', { class: 'row small' }, skip, 'Skip topics I already marked as done'),
        estimate,
        h('div', { class: 'row' }, h('button', { class: 'btn primary', type: 'submit', text: 'Generate plan' }),
            existing ? h('button', { class: 'btn', type: 'button', onclick: () => location.reload(), text: 'Cancel' }) : null));
}

function planView(plan, showForm) {
    const delta = plan.scheduleDeltaHours;
    return h('div', { class: 'stack' },
        h('div', { class: 'card' },
            h('div', { class: 'row' },
                h('div', {}, h('h2', { text: `${plan.totalWeeks}-week plan at ${plan.hoursPerWeek} h/week` }),
                    h('div', { class: 'muted small', text: `${formatDate(plan.startDate)} -> ${formatDate(plan.endDate)} - you are in week ${plan.currentWeek}` })),
                h('div', { class: 'row right' },
                    h('span', { class: 'badge', text: delta >= 0 ? (delta > 0 ? `${delta} h ahead` : 'On track') : `${-delta} h behind` }),
                    h('button', { class: 'btn small', onclick: showForm, text: 'Re-plan' }),
                    h('button', { class: 'btn small danger', text: 'Archive', onclick: async () => {
                        if (!confirm('Archive this plan? Your topic progress is kept.')) return;
                        await api('/api/plans/current', { method: 'DELETE' });
                        showForm();
                    } }))),
            h('div', { style: { marginTop: '.75rem' } }, progressBar(plan.percentDone)),
            h('div', { class: 'muted small', text: `${plan.percentDone}% of planned hours completed` })),
        plan.weeks.map(w => h('details', { class: `week ${w.week === plan.currentWeek ? 'current' : ''}`, open: w.week === plan.currentWeek },
            h('summary', {},
                h('strong', { text: `Week ${w.week}` }),
                h('span', { class: 'muted small', text: `${formatDate(w.startDate)} - ${formatDate(w.endDate)}` }),
                w.week === plan.currentWeek ? h('span', { class: 'badge star', text: 'This week' }) : null,
                h('span', { class: 'muted small right', text: `${w.plannedHours} h planned - ${Math.round(w.minutesLogged / 6) / 10} h logged` }),
                h('span', { class: 'badge', text: `${w.items.filter(i => i.done).length}/${w.items.length}` })),
            h('div', { class: 'week-body' }, w.items.map(planItem)))));
}

function sessionsTable(sessions) {
    if (!sessions.length) return h('p', { class: 'muted', text: 'No sessions yet - log time from the dashboard.' });
    const titles = new Map(allTopics().map(t => [t.id, t.title]));
    const body = h('tbody', {}, sessions.map(s => {
        const row = h('tr', {},
            h('td', { text: formatDate(s.date) }),
            h('td', { text: `${s.minutes} min` }),
            h('td', { text: s.topicId ? titles.get(s.topicId) || '' : '-' }),
            h('td', { class: 'small', text: s.note || '' }),
            h('td', {}, h('button', { class: 'btn small danger', text: 'Delete', onclick: async () => {
                try { await api(`/api/sessions/${s.id}`, { method: 'DELETE' }); row.remove(); } catch (e) { toast(e.message, 'error'); }
            } })));
        return row;
    }));
    return h('div', { class: 'table-wrap' }, h('table', {},
        h('thead', {}, h('tr', {}, ['Date', 'Time', 'Topic', 'Note', ''].map(t => h('th', { text: t })))), body));
}
