import { api } from '../api.js';
import { field, formatDate, h, mount, progressBar, select, toast, today } from '../dom.js';
import { allTopics, loadProgress, loadRoadmap, setTopicStatus, state } from '../state.js';

export async function renderDashboard(root) {
    const [, progress, plan, heat, projects] = await Promise.all([
        loadRoadmap(),
        loadProgress(),
        api('/api/plans/current').catch(e => (e.status === 404 ? null : Promise.reject(e))),
        api('/api/sessions/heatmap?days=112'),
        api('/api/lab/projects'),
    ]);

    const nextTopic = allTopics().find(t => !progress.topics.some(p => p.topicId === t.id && p.status === 'DONE'));

    mount(root,
        h('div', { class: 'page-head' },
            h('div', {},
                h('h1', { text: `Namaste, ${state.user.displayName}` }),
                h('p', { text: 'Learn -> Build -> Break -> Debug -> Improve -> Repeat' })),
            nextTopic ? h('a', { class: 'btn primary', href: '#/roadmap', text: `Next up: ${nextTopic.title}` }) : null),
        h('div', { class: 'grid cols-4' },
            stat('Roadmap done', `${progress.percent}%`, `${progress.hoursDone} of ${progress.hoursTotal} planned hours`, progressBar(progress.percent)),
            stat('Topics done', `${progress.topicsDone}`, `of ${progress.topicsTotal} topics`),
            stat('Study streak', `${progress.currentStreakDays} ${progress.currentStreakDays === 1 ? 'day' : 'days'}`, 'log time daily to keep it going'),
            stat('Last 7 days', `${Math.round(progress.minutesLast7Days / 6) / 10} h`, `${progress.minutesLast7Days} minutes logged`)),
        h('div', { class: 'grid cols-2', style: { marginTop: '1rem' } },
            thisWeek(plan),
            h('div', { class: 'stack' }, logSession(), heatmap(heat))),
        h('div', { class: 'card', style: { marginTop: '1rem' } },
            h('h2', { text: 'Levels' }),
            h('div', { class: 'grid cols-2' }, progress.levels.map(l => h('div', {},
                h('div', { class: 'row small' }, h('strong', { text: `L${l.levelNumber}` }), l.title,
                    h('span', { class: 'right muted', text: `${l.topicsDone}/${l.topicsTotal}` })),
                progressBar(l.percent))))),
        h('div', { class: 'card', style: { marginTop: '1rem' } },
            h('div', { class: 'row' }, h('h2', { text: 'Rebuild Lab' }), h('a', { class: 'btn small right', href: '#/lab', text: 'Open lab' })),
            projects.length
                ? h('div', { class: 'stack' }, projects.slice(0, 4).map(p => h('a', { class: 'file-row', href: `#/lab/p/${p.id}` },
                    h('span', {}, h('strong', { text: p.name }), ' ', p.template ? h('span', { class: 'badge', text: 'template' }) : null),
                    h('span', { class: 'muted small', text: `${p.fileCount} files` }),
                    h('span', { class: 'muted small', text: `${p.totalLines} lines` }))))
                : h('p', { class: 'muted', text: 'Upload your Spring Boot project as a .zip to rebuild it line by line.' })));

    function stat(label, value, sub, extra) {
        return h('div', { class: 'card stat' }, h('div', { class: 'label', text: label }), h('div', { class: 'value', text: value }),
            h('div', { class: 'sub', text: sub }), extra);
    }
}

function thisWeek(plan) {
    const card = h('div', { class: 'card' });
    if (!plan) {
        return mount(card, h('h2', { text: 'This week' }),
            h('p', { class: 'muted', text: 'You have no study plan yet. Generate one from the roadmap in 10 seconds.' }),
            h('a', { class: 'btn primary', href: '#/planner', text: 'Create my plan' }));
    }
    const week = plan.weeks.find(w => w.week === plan.currentWeek) || plan.weeks[0];
    const delta = plan.scheduleDeltaHours;
    return mount(card,
        h('div', { class: 'row' }, h('h2', { text: `Week ${week.week} of ${plan.totalWeeks}` }),
            h('span', { class: 'badge right', text: delta >= 0 ? (delta > 0 ? `${delta} h ahead` : 'on track') : `${-delta} h behind` })),
        h('p', { class: 'muted small', text: `${formatDate(week.startDate)} - ${formatDate(week.endDate)} - ${week.plannedHours} h planned` }),
        week.items.map(item => planItem(item)),
        h('a', { class: 'btn small', href: '#/planner', text: 'Full plan' }));
}

export function planItem(item) {
    const box = h('input', { type: 'checkbox', checked: item.done, 'aria-label': `Mark ${item.topicTitle} done` });
    const row = h('label', { class: `plan-item ${item.done ? 'done' : ''}` }, box,
        h('span', { class: 't', text: item.topicTitle }),
        h('span', { class: 'badge', text: `L${item.levelNumber}` }),
        h('span', { class: 'muted small right', text: `${item.plannedHours} h` }));
    box.addEventListener('change', async () => {
        try {
            await setTopicStatus(item.topicId, box.checked ? 'DONE' : 'IN_PROGRESS');
            item.done = box.checked;
            row.classList.toggle('done', box.checked);
            if (box.checked) toast('Nice! Topic marked as done');
        } catch (e) {
            box.checked = !box.checked;
            toast(e.message, 'error');
        }
    });
    return row;
}

function logSession() {
    const minutes = h('input', { type: 'number', min: 1, max: 720, value: 60, required: true });
    const date = h('input', { type: 'date', value: today(), max: today(), required: true });
    const topic = select([['', '(any topic)'], ...allTopics().map(t => [t.id, `L${t.level.levelNumber} - ${t.title}`])], '');
    const note = h('input', { type: 'text', maxlength: 500, placeholder: 'What did you learn?' });
    return h('form', { class: 'card stack', onsubmit: async e => {
        e.preventDefault();
        try {
            await api('/api/sessions', { method: 'POST', body: {
                minutes: Number(minutes.value), date: date.value, topicId: topic.value ? Number(topic.value) : null, note: note.value || null } });
            toast(`Logged ${minutes.value} minutes`);
            location.hash = '#/dashboard';
            window.dispatchEvent(new HashChangeEvent('hashchange'));
        } catch (err) {
            toast(err.message, 'error');
        }
    } },
        h('h2', { text: 'Log study time' }),
        h('div', { class: 'row' }, field('Minutes', minutes), field('Date', date)),
        field('Topic', topic), field('Note', note),
        h('div', {}, h('button', { class: 'btn primary', type: 'submit', text: 'Log time' })));
}

function heatmap(days) {
    const byDay = new Map(days.map(d => [d.date, d.minutes]));
    const cells = [];
    const end = new Date();
    const start = new Date(end);
    start.setDate(end.getDate() - 111 - end.getDay()); // align columns to weeks starting Sunday
    for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
        const key = new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
        const m = byDay.get(key) || 0;
        const level = m === 0 ? 0 : m < 30 ? 1 : m < 60 ? 2 : m < 120 ? 3 : 4;
        cells.push(h('span', { 'data-l': level, title: `${key}: ${m} min` }));
    }
    return h('div', { class: 'card' }, h('h2', { text: 'Consistency' }),
        h('div', { class: 'heatmap', role: 'img', 'aria-label': 'Study minutes per day for the last 16 weeks' }, cells),
        h('p', { class: 'muted small', text: 'Each square is a day. Darker = more minutes studied.' }));
}
