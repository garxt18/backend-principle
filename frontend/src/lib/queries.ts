import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, ApiError } from './api';
import type {
  DayMinutes,
  Level,
  MyLink,
  MyResources,
  Plan,
  ProgressSummary,
  StudySession,
  TopicProgress,
  TopicStatus,
} from './types';

export const keys = {
  roadmap: ['roadmap'] as const,
  progress: ['progress'] as const,
  plan: ['plan'] as const,
  sessions: ['sessions'] as const,
  heatmap: ['heatmap'] as const,
  myResources: ['me', 'resources'] as const,
  labProjects: ['lab', 'projects'] as const,
  labProject: (id: string) => ['lab', 'project', id] as const,
  labFile: (id: string) => ['lab', 'file', id] as const,
};

export const useRoadmap = () => useQuery({ queryKey: keys.roadmap, queryFn: () => api<Level[]>('/api/roadmap'), staleTime: 5 * 60_000 });
export const useProgress = () => useQuery({ queryKey: keys.progress, queryFn: () => api<ProgressSummary>('/api/progress') });
export const useSessions = () => useQuery({ queryKey: keys.sessions, queryFn: () => api<StudySession[]>('/api/sessions') });
export const useHeatmap = () => useQuery({ queryKey: keys.heatmap, queryFn: () => api<DayMinutes[]>('/api/sessions/heatmap?days=140') });
export const useMyResources = () => useQuery({ queryKey: keys.myResources, queryFn: () => api<MyResources>('/api/me/resources') });

/** "No plan yet" is a normal state, not an error: map 404 to null. */
export const usePlan = () =>
  useQuery({
    queryKey: keys.plan,
    queryFn: async () => {
      try {
        return await api<Plan>('/api/plans/current');
      } catch (e) {
        if (e instanceof ApiError && e.status === 404) return null;
        throw e;
      }
    },
  });

/** Topic status with an optimistic update: the UI changes instantly and rolls back if the server says no. */
export function useSetTopicStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { topicId: number; status?: TopicStatus; notes?: string; confidence?: number | null }) =>
      api<TopicProgress>(`/api/progress/topics/${v.topicId}`, { method: 'PUT', body: v }),
    onMutate: async (v) => {
      await qc.cancelQueries({ queryKey: keys.progress });
      const previous = qc.getQueryData<ProgressSummary>(keys.progress);
      if (previous && v.status) {
        const others = previous.topics.filter((t) => t.topicId !== v.topicId);
        const current = previous.topics.find((t) => t.topicId === v.topicId);
        qc.setQueryData<ProgressSummary>(keys.progress, {
          ...previous,
          topics: [...others, { ...(current ?? emptyProgress(v.topicId)), status: v.status }],
        });
      }
      return { previous };
    },
    onError: (_e, _v, ctx) => ctx?.previous && qc.setQueryData(keys.progress, ctx.previous),
    onSettled: () => {
      qc.invalidateQueries({ queryKey: keys.progress });
      qc.invalidateQueries({ queryKey: keys.plan });
    },
  });
}

function emptyProgress(topicId: number): TopicProgress {
  return { topicId, status: 'NOT_STARTED', confidence: null, notes: null, startedAt: null, completedAt: null, updatedAt: '' };
}

/** Follow one resource per level ("this is the one I'm going to follow"). null = stop following. */
export function useFollow() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { levelId: number; resourceId?: number; userResourceId?: number } | { levelId: number; clear: true }) =>
      'clear' in v
        ? api<void>(`/api/me/resources/follow/${v.levelId}`, { method: 'DELETE' })
        : api(`/api/me/resources/follow/${v.levelId}`, { method: 'PUT', body: { resourceId: v.resourceId ?? null, userResourceId: v.userResourceId ?? null } }),
    onSettled: () => qc.invalidateQueries({ queryKey: keys.myResources }),
  });
}

export function useAddLink() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { levelId?: number; topicId?: number; title: string; url: string; note?: string }) =>
      api<MyLink>('/api/me/resources/links', { method: 'POST', body: v }),
    onSettled: () => qc.invalidateQueries({ queryKey: keys.myResources }),
  });
}

export function useDeleteLink() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => api<void>(`/api/me/resources/links/${id}`, { method: 'DELETE' }),
    onSettled: () => qc.invalidateQueries({ queryKey: keys.myResources }),
  });
}
