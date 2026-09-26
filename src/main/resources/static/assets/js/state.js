import { api } from './api.js';

// Small shared cache so switching pages does not refetch the (large) roadmap every time.
export const state = {
    user: null,
    roadmap: null,
    progress: null,
};

export async function loadRoadmap(force = false) {
    if (!state.roadmap || force) state.roadmap = await api('/api/roadmap');
    return state.roadmap;
}

export async function loadProgress() {
    state.progress = await api('/api/progress');
    return state.progress;
}

export function progressByTopic() {
    const map = new Map();
    for (const p of state.progress?.topics || []) map.set(p.topicId, p);
    return map;
}

export async function setTopicStatus(topicId, status) {
    const updated = await api(`/api/progress/topics/${topicId}`, { method: 'PUT', body: { status } });
    if (state.progress) {
        const others = state.progress.topics.filter(t => t.topicId !== topicId);
        state.progress.topics = [...others, updated];
    }
    return updated;
}

export function allTopics() {
    return (state.roadmap || []).flatMap(level => level.topics.map(t => ({ ...t, level })));
}

export function languageMatches(resourceLanguage) {
    const pref = state.user?.preferredLanguage || 'BOTH';
    if (pref === 'BOTH') return true;
    return pref === 'HINDI' ? resourceLanguage === 'HI' : resourceLanguage === 'EN';
}
