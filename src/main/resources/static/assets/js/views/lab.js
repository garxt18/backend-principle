import { api } from '../api.js';
import { field, formatDate, h, markdown, mount, progressBar, toast } from '../dom.js';

// ------------------------------------------------------------------ projects list + upload

export async function renderLabHome(root) {
    const projects = await api('/api/lab/projects');
    const templates = projects.filter(p => p.template);
    const mine = projects.filter(p => !p.template);

    mount(root,
        h('div', { class: 'page-head' },
            h('div', {}, h('h1', { text: 'Rebuild Lab' }),
                h('p', { text: 'Retype a real project line by line, in the order a senior engineer would build it - with an explanation for every line.' }))),
        h('div', { class: 'grid cols-2' },
            h('div', { class: 'stack' },
                h('h2', { text: 'Templates' }),
                templates.map(projectCard),
                h('h2', { text: 'Your projects' }),
                mine.length ? mine.map(projectCard) : h('p', { class: 'muted', text: 'Nothing uploaded yet.' })),
            uploadForm()));
}

function projectCard(p) {
    return h('div', { class: 'card' },
        h('div', { class: 'row' }, h('h3', { text: p.name }), p.template ? h('span', { class: 'badge', text: 'template' }) : null),
        p.description ? h('p', { class: 'small muted', text: p.description }) : null,
        h('div', { class: 'row small muted' }, `${p.fileCount} files - ${p.totalLines} lines`, p.template ? null : ` - uploaded ${formatDate(p.createdAt)}`),
        h('div', { class: 'row', style: { marginTop: '.5rem' } },
            h('a', { class: 'btn primary small', href: `#/lab/p/${p.id}`, text: 'Open' }),
            p.template ? null : h('button', { class: 'btn small danger', text: 'Delete', onclick: async () => {
                if (!confirm(`Delete "${p.name}" and your progress on it?`)) return;
                try { await api(`/api/lab/projects/${p.id}`, { method: 'DELETE' }); toast('Project deleted'); location.hash = '#/lab'; window.dispatchEvent(new HashChangeEvent('hashchange')); }
                catch (e) { toast(e.message, 'error'); }
            } })));
}

function uploadForm() {
    const file = h('input', { type: 'file', accept: '.zip,application/zip', required: true });
    const name = h('input', { type: 'text', maxlength: 120, required: true, placeholder: 'e.g. My E-Commerce API' });
    const desc = h('textarea', { rows: 3, maxlength: 1000, placeholder: 'What does this project do?' });
    const submit = h('button', { class: 'btn primary', type: 'submit', text: 'Upload & analyse' });
    const result = h('div');
    file.addEventListener('change', () => {
        if (!name.value && file.files[0]) name.value = file.files[0].name.replace(/\.zip$/i, '');
    });
    return h('form', { class: 'card stack', onsubmit: async e => {
        e.preventDefault();
        const form = new FormData();
        form.append('file', file.files[0]);
        form.append('name', name.value);
        if (desc.value) form.append('description', desc.value);
        submit.disabled = true;
        submit.textContent = 'Uploading...';
        try {
            const outcome = await api('/api/lab/projects', { method: 'POST', form });
            toast(`Imported ${outcome.project.fileCount} files`);
            if (outcome.skipped.length) {
                mount(result, h('div', { class: 'note-box small' }, h('strong', { text: 'Skipped: ' }), outcome.skipped.join(', ')));
            }
            location.hash = `#/lab/p/${outcome.project.id}`;
        } catch (err) {
            mount(result, h('div', { class: 'error-box', text: err.message }));
        } finally {
            submit.disabled = false;
            submit.textContent = 'Upload & analyse';
        }
    } },
        h('h2', { text: 'Upload your project' }),
        h('p', { class: 'small muted', text: 'Zip your project folder (or download a GitHub repo as ZIP). Source files (.java, .xml, .yml, .properties, .sql, Dockerfile, ...) are imported; target/, build/, .git/ and node_modules/ are ignored. Max 5 MB. Only you can see your uploads.' }),
        field('Project zip', file), field('Name', name), field('Description (optional)', desc), result,
        h('div', {}, submit));
}

// ------------------------------------------------------------------ project overview

export async function renderLabProject(root, projectId) {
    const d = await api(`/api/lab/projects/${projectId}`);
    const next = d.files.find(f => !f.completed) || d.files[0];
    const byLayer = new Map();
    d.files.forEach(f => { if (!byLayer.has(f.layer)) byLayer.set(f.layer, []); byLayer.get(f.layer).push(f); });

    mount(root,
        h('div', { class: 'page-head' },
            h('div', {}, h('a', { href: '#/lab', class: 'small', text: '<- Rebuild Lab' }), h('h1', { text: d.project.name }),
                h('p', { text: d.project.description || `${d.project.fileCount} files, ${d.project.totalLines} lines` })),
            next ? h('a', { class: 'btn primary', href: `#/lab/f/${next.id}`, text: d.linesCompleted ? 'Continue rebuilding' : 'Start rebuilding' }) : null),
        h('div', { class: 'card', style: { marginBottom: '1rem' } },
            h('div', { class: 'row small' }, h('strong', { text: `${d.percent}% rebuilt` }), h('span', { class: 'muted right', text: `${d.linesCompleted} / ${d.project.totalLines} lines` })),
            progressBar(d.percent),
            h('p', { class: 'small muted', style: { marginTop: '.5rem' }, text: 'Files are ordered so you never type code that uses something you have not written yet: build file -> config -> schema -> entities -> repositories -> DTOs -> services -> controllers -> tests -> infrastructure.' })),
        d.layers.map((layer, i) => h('div', { class: 'card layer-block' },
            h('h3', {}, h('span', { class: 'badge', text: `Step ${i + 1}` }), layer.label),
            h('p', { class: 'small muted', text: layer.why }),
            byLayer.get(layer.layer).map(f => h('a', { class: 'file-row', href: `#/lab/f/${f.id}` },
                h('span', { class: 'path', text: `${f.completed ? '[done] ' : ''}${f.path}` }),
                progressBar(f.lineCount ? Math.round(f.linesCompleted * 100 / f.lineCount) : 100),
                h('span', { class: 'muted small', text: `${f.linesCompleted}/${f.lineCount}` }))))));
}

// ------------------------------------------------------------------ the rebuild editor

const normalize = s => s.trim().replace(/\s+/g, ' ');

export async function renderLabFile(root, fileId) {
    const file = await api(`/api/lab/files/${fileId}`);
    const lines = file.lines;
    const explanations = new Map();
    let mode = 'type';
    let showTarget = true;
    let cursor = skipBlank(Math.min(file.linesCompleted, lines.length));
    let selected = cursor < lines.length ? cursor : 0;
    let saveTimer = null;
    let lastSaved = file.linesCompleted;

    const codeBox = h('div', { class: 'code', role: 'list', 'aria-label': 'File content' });
    const typer = h('div', { class: 'typer' });
    const explainBox = h('div', { class: 'card explain sticky' });
    const progressSlot = h('div');
    const modeBtn = h('button', { class: 'btn small', onclick: () => { mode = mode === 'type' ? 'read' : 'type'; drawAll(); } });
    const revealBtn = h('button', { class: 'btn small', onclick: () => { showTarget = !showTarget; drawAll(); } });

    function skipBlank(i) {
        while (i < lines.length && lines[i].trim() === '') i++;
        return i;
    }

    function scheduleSave() {
        clearTimeout(saveTimer);
        saveTimer = setTimeout(async () => {
            if (cursor === lastSaved) return;
            try {
                await api(`/api/lab/files/${fileId}/progress`, { method: 'PUT', body: { linesCompleted: cursor } });
                lastSaved = cursor;
            } catch (e) { toast(`Progress not saved: ${e.message}`, 'error'); }
        }, 800);
    }

    function drawCode() {
        mount(codeBox, lines.map((line, i) => {
            const cls = ['code-line'];
            if (mode === 'type') {
                if (i < cursor) cls.push('done');
                if (i === cursor) cls.push('current');
                if (i > cursor || (i === cursor && !showTarget)) cls.push('hidden');
            }
            if (i === selected) cls.push('selected');
            return h('div', { class: cls.join(' '), role: 'listitem', onclick: () => { selected = i; drawCode(); drawExplain(); } },
                h('span', { class: 'ln', text: i + 1 }), h('span', { class: 'src', text: line || ' ' }));
        }));
        codeBox.querySelector('.current, .selected')?.scrollIntoView({ block: 'nearest' });
    }

    function drawProgress() {
        const pct = lines.length ? Math.round(cursor * 100 / lines.length) : 100;
        mount(progressSlot, h('div', { class: 'row small' }, h('strong', { text: `${cursor} / ${lines.length} lines` }), h('span', { class: 'muted right', text: `${pct}%` })), progressBar(pct));
    }

    function drawTyper() {
        if (mode !== 'type') { mount(typer, h('p', { class: 'muted small', text: 'Read mode: click any line to see its explanation.' })); return; }
        if (cursor >= lines.length) {
            mount(typer, h('div', { class: 'note-box' }, h('strong', { text: 'File rebuilt!' }), ' Explain it out loud before moving on. ',
                file.nextFileId ? h('a', { class: 'btn primary small', href: `#/lab/f/${file.nextFileId}`, text: 'Next file ->' })
                    : h('a', { class: 'btn primary small', href: `#/lab/p/${file.projectId}`, text: 'Project complete - back to overview' }),
                ' ', h('button', { class: 'btn small', text: 'Retype this file', onclick: () => { cursor = skipBlank(0); selected = cursor; scheduleSave(); drawAll(); } })));
            return;
        }
        const target = lines[cursor];
        const targetBox = h('div', { class: 'target', 'aria-label': 'Line to type' });
        const input = h('input', { type: 'text', autocomplete: 'off', autocapitalize: 'off', spellcheck: 'false',
            'aria-label': `Type line ${cursor + 1}`, placeholder: showTarget ? 'Type the highlighted line (indentation is optional), then press Enter' : 'Type the line from memory - press Esc to peek' });

        const paint = () => {
            const want = normalize(target);
            const got = normalize(input.value);
            let ok = 0;
            while (ok < got.length && ok < want.length && got[ok] === want[ok]) ok++;
            const matched = got === want;
            input.classList.toggle('match', matched);
            if (!showTarget) {
                mount(targetBox, h('span', { class: 'muted', text: matched ? 'Correct - press Enter' : `${ok} characters correct so far` }));
                return;
            }
            mount(targetBox,
                h('span', { class: 'ok', text: want.slice(0, ok) }),
                ok < got.length ? h('span', { class: 'bad', text: want.slice(ok, ok + 1) || ' ' }) : null,
                h('span', { text: want.slice(ok < got.length ? ok + 1 : ok) }));
        };
        input.addEventListener('input', paint);
        input.addEventListener('keydown', e => {
            if (e.key === 'Enter') {
                e.preventDefault();
                if (normalize(input.value) === normalize(target)) advance();
                else toast('Not quite - compare with the highlighted characters', 'error');
            } else if (e.key === 'Escape') {
                showTarget = !showTarget;
                drawAll();
            }
        });
        paint();
        mount(typer, h('div', { class: 'row small muted' }, `Line ${cursor + 1}`, h('span', { class: 'kbd', text: 'Enter' }), 'next',
            h('span', { class: 'kbd', text: 'Esc' }), 'hide/peek'), targetBox, input,
            h('div', { class: 'row', style: { marginTop: '.5rem' } },
                h('button', { class: 'btn small', text: 'Skip line', onclick: advance }),
                h('button', { class: 'btn small', text: 'Back one line', disabled: cursor === 0, onclick: () => { cursor = Math.max(0, cursor - 1); selected = cursor; scheduleSave(); drawAll(); } })));
        input.focus();
    }

    function advance() {
        cursor = skipBlank(cursor + 1);
        selected = Math.min(cursor, lines.length - 1);
        scheduleSave();
        drawAll();
    }

    async function drawExplain() {
        const lineNo = selected + 1;
        if (!lines.length) { mount(explainBox, h('p', { class: 'muted', text: 'Empty file.' })); return; }
        mount(explainBox, h('p', { class: 'muted small', text: `Explaining line ${lineNo}...` }));
        try {
            if (!explanations.has(lineNo)) explanations.set(lineNo, await api(`/api/lab/files/${fileId}/lines/${lineNo}/explain`));
            if (selected + 1 !== lineNo) return; // user moved on meanwhile
            const ex = explanations.get(lineNo);
            const aiSlot = h('div');
            mount(explainBox,
                h('div', { class: 'kind', text: `Line ${lineNo} - ${ex.kind}` }),
                h('pre', { class: 'code', text: ex.code.trim() || '(blank)' }),
                h('p', { text: ex.summary }),
                ex.context ? h('p', { class: 'small muted', text: `Context: ${ex.context}` }) : null,
                ex.notes.length ? h('dl', {}, ex.notes.map(n => [h('dt', { text: n.term }), h('dd', { text: n.text })])) : null,
                h('div', { class: 'row', style: { marginTop: '.75rem' } },
                    h('button', { class: 'btn small primary', text: 'Ask AI mentor about this line', onclick: async e => {
                        e.target.disabled = true;
                        mount(aiSlot, h('p', { class: 'muted small', text: 'Thinking...' }));
                        try {
                            const a = await api(`/api/mentor/explain/${fileId}/${lineNo}`, { method: 'POST' });
                            mount(aiSlot, h('h3', { text: a.cached ? 'AI mentor (cached)' : 'AI mentor' }), markdown(a.answer),
                                h('p', { class: 'muted small', text: `AI questions today: ${a.usedToday}/${a.dailyQuota}` }));
                        } catch (err) {
                            mount(aiSlot, h('div', { class: 'error-box small', text: err.message }));
                        } finally { e.target.disabled = false; }
                    } })),
                aiSlot);
        } catch (e) {
            mount(explainBox, h('div', { class: 'error-box', text: e.message }));
        }
    }

    function drawAll() {
        modeBtn.textContent = mode === 'type' ? 'Switch to read mode' : 'Switch to type mode';
        revealBtn.textContent = showTarget ? 'Blind mode' : 'Show target';
        revealBtn.hidden = mode !== 'type';
        drawCode();
        drawTyper();
        drawProgress();
        drawExplain();
    }

    mount(root,
        h('div', { class: 'page-head' },
            h('div', {},
                h('a', { href: `#/lab/p/${file.projectId}`, class: 'small', text: '<- Project overview' }),
                h('h1', { class: 'mono', style: { fontSize: '1.1rem', overflowWrap: 'anywhere' }, text: file.path }),
                h('p', {}, h('span', { class: 'badge', text: file.layerLabel }), ' ', file.layerWhy)),
            h('div', { class: 'row' },
                file.previousFileId ? h('a', { class: 'btn small', href: `#/lab/f/${file.previousFileId}`, text: '<- Prev file' }) : null,
                file.nextFileId ? h('a', { class: 'btn small', href: `#/lab/f/${file.nextFileId}`, text: 'Next file ->' }) : null)),
        h('div', { class: 'lab-layout' },
            h('div', { class: 'stack' },
                h('div', { class: 'card' }, progressSlot, h('div', { class: 'row', style: { marginTop: '.5rem' } }, modeBtn, revealBtn)),
                typer,
                codeBox),
            explainBox));
    drawAll();
    // Leaving the page: save immediately instead of waiting for the debounce timer.
    window.addEventListener('hashchange', () => {
        clearTimeout(saveTimer);
        if (cursor !== lastSaved) {
            api(`/api/lab/files/${fileId}/progress`, { method: 'PUT', body: { linesCompleted: cursor } }).catch(() => {});
        }
    }, { once: true });
}
