import type { Level, MyLink, MyResources, Topic } from './types';

export const STRIVER_A2Z = 'https://takeuforward.org/dsa/strivers-a2z-sheet-learn-dsa-a-to-z';
export const STRIVER_PLAYLIST = 'https://www.youtube.com/playlist?list=PLgUwDviBIf0oF6QL8m22w1hIDC1vJ_BHz';

export interface Followed {
  title: string;
  url: string;
  channel: string | null;
  own: boolean;
}

/** The resource the learner follows for a level: their explicit choice, else the level's playlist. */
export function followedFor(level: Level, mine: MyResources | undefined): Followed | null {
  const choice = mine?.choices.find((c) => c.levelId === level.id);
  if (choice?.resourceId) {
    const r = level.resources.find((x) => x.id === choice.resourceId);
    if (r) return { title: r.title, url: r.url, channel: r.channel, own: false };
  }
  if (choice?.userResourceId) {
    const l = mine?.links.find((x) => x.id === choice.userResourceId);
    if (l) return { title: l.title, url: l.url, channel: null, own: true };
  }
  if (level.playlist) return { title: level.playlist.name, url: level.playlist.url, channel: level.playlist.channel, own: false };
  return null;
}

export interface Watch {
  url: string;
  label: string;
  /** mine = your own link for this topic, lecture = exact lecture video, playlist = playlist fallback, followed = your chosen resource */
  source: 'mine' | 'lecture' | 'playlist' | 'followed';
}

/** Where "Watch" goes for one topic: your own link > exact lecture > playlist > the resource you follow. */
export function watchFor(level: Level, topic: Topic, mine: MyResources | undefined): Watch | null {
  const own = topicLinks(topic.id, mine)[0];
  if (own) return { url: own.url, label: 'Your link', source: 'mine' };
  if (topic.videoUrl) return { url: topic.videoUrl, label: topic.lectureNumber ? `Lecture ${topic.lectureNumber}` : 'Watch', source: 'lecture' };
  if (level.playlist && topic.lectureNumber) return { url: level.playlist.url, label: `Playlist · lecture ${topic.lectureNumber}`, source: 'playlist' };
  const f = followedFor(level, mine);
  if (f) return { url: f.url, label: f.own ? 'Your resource' : 'Followed resource', source: 'followed' };
  return null;
}

export function topicLinks(topicId: number, mine: MyResources | undefined): MyLink[] {
  return mine?.links.filter((l) => l.topicId === topicId) ?? [];
}

export function levelLinks(levelId: number, mine: MyResources | undefined): MyLink[] {
  return mine?.links.filter((l) => l.levelId === levelId && l.topicId === null) ?? [];
}

export function isYoutube(url: string) {
  return /(^https?:\/\/)?(www\.|m\.)?(youtube\.com|youtu\.be)\//i.test(url);
}
