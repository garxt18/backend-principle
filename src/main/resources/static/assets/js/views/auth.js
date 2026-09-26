import { api } from '../api.js';
import { field, h, mount, select } from '../dom.js';
import { loggedIn } from '../app.js';

export function renderAuth(root) {
    let mode = 'login';
    const box = h('div', { class: 'card' });

    function draw() {
        const errors = h('div');
        const email = h('input', { type: 'email', required: true, autocomplete: 'email' });
        const password = h('input', { type: 'password', required: true, minlength: mode === 'register' ? 8 : null,
            autocomplete: mode === 'login' ? 'current-password' : 'new-password' });
        const name = h('input', { type: 'text', required: true, minlength: 2, maxlength: 80, autocomplete: 'name' });
        const lang = select([['BOTH', 'Hindi + English'], ['HINDI', 'Hindi / Hinglish'], ['ENGLISH', 'English']], 'BOTH');
        const submit = h('button', { class: 'btn primary', type: 'submit', text: mode === 'login' ? 'Log in' : 'Create account' });

        const form = h('form', { class: 'stack', onsubmit: async e => {
            e.preventDefault();
            submit.disabled = true;
            mount(errors);
            try {
                const body = mode === 'login'
                    ? { email: email.value, password: password.value }
                    : { email: email.value, password: password.value, displayName: name.value, preferredLanguage: lang.value };
                loggedIn(await api(`/api/auth/${mode}`, { method: 'POST', body }));
            } catch (err) {
                const details = Object.entries(err.fieldErrors || {}).map(([f, m]) => `${f}: ${m}`);
                mount(errors, h('div', { class: 'error-box' }, err.message, details.length ? h('div', { class: 'small', text: details.join(' - ') }) : null));
            } finally {
                submit.disabled = false;
            }
        } },
            mode === 'register' ? field('Your name', name) : null,
            field('Email', email),
            field(mode === 'register' ? 'Password (min 8 characters)' : 'Password', password),
            mode === 'register' ? field('Explain things to me in', lang) : null,
            errors,
            submit);

        mount(box,
            h('h1', { text: 'Backend Playground' }),
            h('p', { class: 'intro', text: 'Plan your backend roadmap, track progress, and rebuild real Spring Boot projects line by line with an AI mentor.' }),
            h('div', { class: 'tabs', role: 'tablist' },
                h('button', { class: mode === 'login' ? 'active' : '', role: 'tab', onclick: () => { mode = 'login'; draw(); }, text: 'Log in' }),
                h('button', { class: mode === 'register' ? 'active' : '', role: 'tab', onclick: () => { mode = 'register'; draw(); }, text: 'Sign up' })),
            form);
        (mode === 'register' ? name : email).focus();
    }

    mount(root, h('div', { class: 'auth' }, box));
    draw();
}
