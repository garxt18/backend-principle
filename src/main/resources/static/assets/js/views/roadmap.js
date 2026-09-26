import { api } from '../api.js';
import { h, mount, progressBar, select, toast } from '../dom.js';
import { languageMatches, loadProgress, loadRoadmap, progressByTopic, state } from '../state.js';

const KIND_LABEL = { PLAYLIST: 'Playlist', VIDEO: 'Video', CHANNEL: 'Channel', COURSE: 'Course', DOCS: 'Docs', SEARCH: 'Search' };

export async function renderRoadmap(root) {
    const [levels] = await Promise.all([loadRoadmap(), loadProgress()]);
    let langFilter = state.user.preferredLanguage === 'BOTH' ? 'ALL' : state.user.preferredLanguage === 'HINDI' ? 'HI' : 'EN';
    const list = h('div');
    const filter = select([['ALL', 'Hindi + English'], ['HI', 'Hindi only'], ['EN', 'English only']], langFilter, {
        'aria-label': 'Resource language', onchange: e => { langFilter = e.target.value; draw(); } });

    function draw() {
        const progress = progressByTopic();
        const summary = new Map(state.progress.levels.map(l => [l.levelId, l]));
        const openLevels = new Set([...list.querySelectorAll('details[open]')].map(d => d.dataset.id));
        mount(list, levels.map(level => levelCard(level, progress, summary.get(level.id), langFilter, openLevels.has(String(level.id)), draw)));
    }

    mount(root,
        h('div', { class: 'page-head' },
            h('div', {}, h('h1', { text: 'Backend Engineering Roadmap' }),
                h('p', { text: 'Java -> Spring Boot -> REST -> SQL -> Security -> Testing -> Redis -> Kafka -> Microservices -> Docker -> Cloud -> K8s -> System Design -> Projects' })),
            h('div', { class: 'row' }, filter)),
        list);
    draw();
}

function levelCard(level, progress, lp, langFilter, open, redraw) {
    const resources = level.resources.filter(r => langFilter === 'ALL' || r.language === langFilter);
    const details = h('details', { class: 'card level', open, dataset: { id: level.id } },
        h('summary', {},
            h('div', { class: 'level-num', text: level.levelNumber }),
            h('div', {}, h('div', { style: { fontWeight: 600 }, text: level.title }),
                h('div', { class: 'level-meta', text: `Month ${level.suggestedMonth ?? '-'} - ${level.totalHours} h - ${level.topics.length} topics` })),
            h('div', { class: 'level-progress' }, progressBar(lp?.percent ?? 0),
                h('div', { class: 'level-meta', text: `${lp?.topicsDone ?? 0}/${level.topics.length} done` }))),
        h('p', { text: level.summary }),
        level.whyItMatters ? h('p', { class: 'muted small' }, h('strong', { text: 'Why it matters: ' }), level.whyItMatters) : null,
        level.projectTitle ? h('div', { class: 'project-box' }, h('strong', { text: `Project: ${level.projectTitle}` }),
            h('div', { class: 'small', text: level.projectDescription })) : null,
        h('h3', { text: 'Resources' }),
        resources.length ? resources.map(resourceRow) : h('p', { class: 'muted small', text: 'No resources in this language - switch the filter.' }),
        h('h3', { style: { marginTop: '1rem' }, text: 'Topics' }),
        level.topics.map(t => topicRow(t, progress.get(t.id), redraw)));
    return details;
}

function resourceRow(r) {
    return h('div', { class: 'resource' },
        h('span', { class: `badge ${r.language === 'HI' ? 'hi' : 'en'}`, text: r.language === 'HI' ? 'Hindi' : 'English' }),
        r.primaryPick ? h('span', { class: 'badge star', text: 'Start here' }) : null,
        h('a', { href: r.url, target: '_blank', rel: 'noopener noreferrer', text: r.title }),
        h('span', { class: 'muted small', text: [KIND_LABEL[r.kind], r.channel].filter(Boolean).join(' - ') }),
        r.note ? h('div', { class: 'muted small', style: { flexBasis: '100%' }, text: r.note }) : null);
}

function topicRow(topic, p, redraw) {
    const status = p?.status || 'NOT_STARTED';
    const statusSelect = select([['NOT_STARTED', 'Not started'], ['IN_PROGRESS', 'In progress'], ['DONE', 'Done']], status, {
        'aria-label': `Status of ${topic.title}`,
        onchange: async e => {
            try {
                await api(`/api/progress/topics/${topic.id}`, { method: 'PUT', body: { status: e.target.value } });
                await loadProgress();
                redraw();
            } catch (err) { toast(err.message, 'error'); }
        } });
    const notes = h('textarea', { rows: 3, maxlength: 10000, placeholder: 'Your notes, links, doubts...', value: p?.notes || '' });
    const confidence = select([['', 'Confidence'], ...[1, 2, 3, 4, 5].map(n => [n, `${n}/5`])], p?.confidence ?? '', { 'aria-label': 'Confidence' });
    const q = encodeURIComponent(topic.title.replace(/[:&/]/g, ' '));
    return h('div', { class: `topic ${status === 'DONE' ? 'done' : ''}` },
        h('div', {},
            h('div', { class: 'topic-title', text: topic.title }),
            h('div', { class: 'small', text: topic.description }),
            topic.practice ? h('div', { class: 'small muted' }, h('strong', { text: 'Practice: ' }), topic.practice) : null,
            h('div', { class: 'row small', style: { marginTop: '.3rem' } },
                h('span', { class: 'muted', text: `${topic.estimatedHours} h` }),
                h('a', { href: `https://www.youtube.com/results?search_query=${q}+in+hindi`, target: '_blank', rel: 'noopener noreferrer', text: 'YouTube (Hindi)' }),
                h('a', { href: `https://www.youtube.com/results?search_query=${q}+java+tutorial`, target: '_blank', rel: 'noopener noreferrer', text: 'YouTube (English)' }),
                h('a', { href: '#/mentor', onclick: () => { state.mentorTopic = topic.id; }, text: 'Ask / quiz me' })),
            h('details', { style: { marginTop: '.3rem' } }, h('summary', { class: 'small muted', text: p?.notes ? 'Notes (saved)' : 'Add notes' }),
                h('div', { class: 'stack', style: { marginTop: '.4rem' } }, notes,
                    h('div', { class: 'row' }, confidence, h('button', { class: 'btn small', text: 'Save notes', onclick: async () => {
                        try {
                            await api(`/api/progress/topics/${topic.id}`, { method: 'PUT', body: {
                                notes: notes.value, confidence: confidence.value ? Number(confidence.value) : null } });
                            await loadProgress();
                            toast('Notes saved');
                        } catch (err) { toast(err.message, 'error'); }
                    } }))))),
        h('div', { class: 'topic-actions' }, statusSelect));
}
