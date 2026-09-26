// Tiny DOM helpers. Every piece of user or server text goes through textContent,
// never innerHTML, so uploaded code or names can never inject HTML (XSS).

export function h(tag, attrs = {}, ...children) {
    const el = document.createElement(tag);
    for (const [key, value] of Object.entries(attrs || {})) {
        if (value === null || value === undefined || value === false) continue;
        if (key === 'class') el.className = value;
        else if (key === 'text') el.textContent = value;
        else if (key === 'dataset') Object.assign(el.dataset, value);
        else if (key === 'style') Object.assign(el.style, value);
        else if (key.startsWith('on') && typeof value === 'function') el.addEventListener(key.slice(2).toLowerCase(), value);
        else if (key === 'value') el.value = value;
        else if (key === 'checked' || key === 'disabled' || key === 'selected' || key === 'open') el[key] = !!value;
        else el.setAttribute(key, value === true ? '' : value);
    }
    append(el, children);
    return el;
}

function append(el, children) {
    for (const child of children.flat(Infinity)) {
        if (child === null || child === undefined || child === false) continue;
        el.append(child instanceof Node ? child : document.createTextNode(String(child)));
    }
}

export function clear(el) {
    while (el.firstChild) el.removeChild(el.firstChild);
    return el;
}

export function mount(el, ...children) {
    clear(el);
    append(el, children);
    return el;
}

export function toast(message, type = 'info') {
    const box = document.getElementById('toasts');
    const t = h('div', { class: `toast ${type}`, text: message });
    box.append(t);
    setTimeout(() => t.remove(), type === 'error' ? 6000 : 3000);
}

export function progressBar(percent) {
    const bar = h('span');
    bar.style.width = `${Math.max(0, Math.min(100, percent))}%`;
    return h('div', { class: 'progress', role: 'progressbar', 'aria-valuenow': percent, 'aria-valuemin': 0, 'aria-valuemax': 100 }, bar);
}

export function field(label, input) {
    return h('label', { class: 'field' }, label, input);
}

export function select(options, value, attrs = {}) {
    return h('select', attrs, options.map(([v, text]) => h('option', { value: v, selected: String(v) === String(value), text })));
}

export function formatDate(iso) {
    if (!iso) return '';
    const d = new Date(iso.length === 10 ? iso + 'T00:00:00' : iso);
    return d.toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
}

export function today() {
    const d = new Date();
    return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
}

/**
 * Minimal, safe Markdown renderer for AI answers: fenced code, headings, lists, **bold** and `code`.
 * It builds DOM nodes (textContent) instead of HTML strings.
 */
export function markdown(text) {
    const root = h('div', { class: 'md' });
    const parts = String(text || '').split(/```/);
    parts.forEach((part, i) => {
        if (i % 2 === 1) {
            const code = part.replace(/^[a-zA-Z0-9+-]*\n/, '');
            root.append(h('pre', {}, h('code', { text: code.replace(/\n$/, '') })));
            return;
        }
        let list = null;
        for (const raw of part.split('\n')) {
            const line = raw.trimEnd();
            const bullet = line.match(/^\s*(?:[-*]|\d+\.)\s+(.*)$/);
            if (bullet) {
                if (!list) { list = h(/^\s*\d/.test(line) ? 'ol' : 'ul'); root.append(list); }
                list.append(h('li', {}, inline(bullet[1])));
                continue;
            }
            list = null;
            if (!line.trim()) continue;
            const heading = line.match(/^#{1,4}\s+(.*)$/);
            root.append(heading ? h('h3', {}, inline(heading[1])) : h('p', {}, inline(line)));
        }
    });
    return root;
}

function inline(text) {
    const out = [];
    const re = /(`[^`]+`|\*\*[^*]+\*\*)/g;
    let last = 0;
    let m;
    while ((m = re.exec(text))) {
        if (m.index > last) out.push(text.slice(last, m.index));
        const token = m[0];
        out.push(token.startsWith('`') ? h('code', { text: token.slice(1, -1) }) : h('strong', { text: token.slice(2, -2) }));
        last = m.index + token.length;
    }
    if (last < text.length) out.push(text.slice(last));
    return out;
}
