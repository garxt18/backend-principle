import { api } from '../api.js';
import { field, h, markdown, mount, select, toast } from '../dom.js';
import { allTopics, loadRoadmap, state } from '../state.js';

export async function renderMentor(root) {
    await loadRoadmap();
    const status = await api('/api/mentor/status');
    const topics = allTopics();
    const preselected = state.mentorTopic || '';
    state.mentorTopic = null;

    const usage = h('span', { class: 'badge', text: `${status.usedToday}/${status.dailyQuota} today` });
    const chat = h('div', { class: 'chat' });
    const topicSelect = select([['', '(no specific topic)'], ...topics.map(t => [t.id, `L${t.level.levelNumber} - ${t.title}`])], preselected);
    const question = h('textarea', { rows: 3, maxlength: 2000, placeholder: 'e.g. Why do we use DTOs instead of returning entities? Samjhao with an example.', required: true });
    const askBtn = h('button', { class: 'btn primary', type: 'submit', text: 'Ask', disabled: !status.enabled });
    const quizBox = h('div');

    const form = h('form', { class: 'card stack', onsubmit: async e => {
        e.preventDefault();
        const q = question.value.trim();
        if (!q) return;
        chat.prepend(h('div', { class: 'msg me', text: q }));
        const pending = h('div', { class: 'msg muted', text: 'Thinking...' });
        chat.prepend(pending);
        askBtn.disabled = true;
        try {
            const a = await api('/api/mentor/ask', { method: 'POST', body: { question: q, topicId: topicSelect.value ? Number(topicSelect.value) : null } });
            mount(pending, markdown(a.answer));
            pending.classList.remove('muted');
            usage.textContent = `${a.usedToday}/${a.dailyQuota} today`;
            question.value = '';
        } catch (err) {
            mount(pending, h('div', { class: 'error-box', text: err.message }));
        } finally { askBtn.disabled = false; }
    } },
        h('div', { class: 'row' }, h('h2', { text: 'Ask anything' }), h('span', { class: 'right' }, usage)),
        field('Topic (optional)', topicSelect), field('Your question (Hindi, Hinglish or English)', question),
        h('div', { class: 'row' }, askBtn,
            h('button', { class: 'btn', type: 'button', disabled: !status.enabled, text: 'Quiz me on this topic', onclick: () => runQuiz() })));

    async function runQuiz() {
        if (!topicSelect.value) { toast('Pick a topic first', 'error'); return; }
        mount(quizBox, h('div', { class: 'card muted', text: 'Generating a 5-question quiz...' }));
        try {
            const quiz = await api(`/api/mentor/quiz/${topicSelect.value}`, { method: 'POST' });
            let score = 0;
            let answered = 0;
            const scoreLine = h('p', { class: 'muted' });
            mount(quizBox, h('div', { class: 'card' }, h('h2', { text: `Quiz: ${quiz.topic}` }),
                quiz.questions.map((q, qi) => {
                    const explanation = h('p', { class: 'small muted', hidden: true, text: q.explanation });
                    const buttons = q.options.map((opt, oi) => h('button', { class: 'btn opt', text: opt, onclick: () => {
                        buttons.forEach((b, bi) => { b.disabled = true; if (bi === q.correctIndex) b.classList.add('right'); });
                        if (oi !== q.correctIndex) buttons[oi].classList.add('wrong'); else score++;
                        answered++;
                        explanation.hidden = false;
                        if (answered === quiz.questions.length) scoreLine.textContent = `Score: ${score}/${quiz.questions.length}`;
                    } }));
                    return h('div', { class: 'quiz-q' }, h('strong', { text: `${qi + 1}. ${q.question}` }), buttons, explanation);
                }), scoreLine));
        } catch (err) {
            mount(quizBox, h('div', { class: 'error-box', text: err.message }));
        }
    }

    mount(root,
        h('div', { class: 'page-head' },
            h('div', {}, h('h1', { text: 'AI Mentor' }),
                h('p', { text: 'A senior Spring Boot engineer on call - built with Spring AI. Answers follow your language preference.' }))),
        status.enabled ? null : h('div', { class: 'note-box', style: { marginBottom: '1rem' } },
            h('strong', { text: 'The AI mentor is not configured on this server. ' }),
            'Start the app with AI_PROVIDER=openai (plus OPENAI_API_KEY), AI_PROVIDER=anthropic (plus ANTHROPIC_API_KEY), or AI_PROVIDER=ollama for a free local model. Everything else in the playground works without it.'),
        h('div', { class: 'grid cols-2' }, form, quizBox),
        h('div', { style: { marginTop: '1rem' } }, chat));
}
