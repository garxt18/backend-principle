import { api } from '../api.js';
import { field, h, mount, select, toast } from '../dom.js';
import { state } from '../state.js';

export async function renderSettings(root) {
    const me = await api('/api/me');
    const name = h('input', { type: 'text', minlength: 2, maxlength: 80, value: me.displayName, required: true });
    const lang = select([['BOTH', 'Hindi + English'], ['HINDI', 'Hindi / Hinglish'], ['ENGLISH', 'English']], me.preferredLanguage);
    const current = h('input', { type: 'password', autocomplete: 'current-password', required: true });
    const next = h('input', { type: 'password', autocomplete: 'new-password', minlength: 8, required: true });

    mount(root,
        h('div', { class: 'page-head' }, h('div', {}, h('h1', { text: 'Settings' }), h('p', { text: me.email }))),
        h('div', { class: 'grid cols-2' },
            h('form', { class: 'card stack', onsubmit: async e => {
                e.preventDefault();
                try {
                    state.user = await api('/api/me', { method: 'PATCH', body: { displayName: name.value, preferredLanguage: lang.value } });
                    toast('Profile saved');
                } catch (err) { toast(err.message, 'error'); }
            } },
                h('h2', { text: 'Profile' }), field('Display name', name),
                field('Resources and AI answers in', lang),
                h('div', {}, h('button', { class: 'btn primary', type: 'submit', text: 'Save' }))),
            h('form', { class: 'card stack', onsubmit: async e => {
                e.preventDefault();
                try {
                    await api('/api/me/password', { method: 'PUT', body: { currentPassword: current.value, newPassword: next.value } });
                    current.value = '';
                    next.value = '';
                    toast('Password changed - other devices were logged out');
                } catch (err) { toast(err.message, 'error'); }
            } },
                h('h2', { text: 'Change password' }), field('Current password', current), field('New password (min 8)', next),
                h('div', {}, h('button', { class: 'btn primary', type: 'submit', text: 'Change password' })))));
}
