import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, ApiError } from './api';
import type {
  DayMinutes,
  DsaProblem,
  DsaSheet,
  Level,
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
  dsa: ['dsa'] as const,
  labProjects: ['lab', 'projects'] as const,
  labProject: (id: string) => ['lab', 'project', id] as const,
  labFile: (id: string) => ['lab', 'file', id] as const,
};

export const useRoadmap = () => useQuery({ queryKey: keys.roadmap, queryFn: () => api<Level[]>('/api/roadmap'), staleTime: 5 * 60_000 });
export const useProgress = () => useQuery({ queryKey: keys.progress, queryFn: () => api<ProgressSummary>('/api/progress') });
export const useSessions = () => useQuery({ queryKey: keys.sessions, queryFn: () => api<StudySession[]>('/api/sessions') });
export const useHeatmap = () => useQuery({ queryKey: keys.heatmap, queryFn: () => api<DayMinutes[]>('/api/sessions/heatmap?days=140') });
export const useDsaSheet = () => useQuery({ queryKey: keys.dsa, queryFn: () => api<DsaSheet>('/api/dsa/sheet') });

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

export function useUpdateProblem() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id: number; solved?: boolean; revision?: boolean; notes?: string }) =>
      api<DsaProblem>(`/api/dsa/problems/${v.id}`, { method: 'PUT', body: v }),
    onMutate: async (v) => {
      await qc.cancelQueries({ queryKey: keys.dsa });
      const previous = qc.getQueryData<DsaSheet>(keys.dsa);
      if (previous) qc.setQueryData(keys.dsa, applyProblemChange(previous, v));
      return { previous };
    },
    onError: (_e, _v, ctx) => ctx?.previous && qc.setQueryData(keys.dsa, ctx.previous),
    onSettled: () => {
      qc.invalidateQueries({ queryKey: keys.dsa });
      qc.invalidateQueries({ queryKey: keys.plan });
    },
  });
}

function applyProblemChange(sheet: DsaSheet, v: { id: number; solved?: boolean; revision?: boolean; notes?: string }): DsaSheet {
  const topics = sheet.topics.map((t) => {
    if (!t.problems.some((p) => p.id === v.id)) return t;
    const problems = t.problems.map((p) =>
      p.id === v.id
        ? { ...p, ...(v.solved !== undefined && { solved: v.solved }), ...(v.revision !== undefined && { revision: v.revision }), ...(v.notes !== undefined && { notes: v.notes }) }
        : p,
    );
    return { ...t, problems, solved: problems.filter((p) => p.solved).length };
  });
  const all = topics.flatMap((t) => t.problems);
  const by = (d: string) => ({ total: all.filter((p) => p.difficulty === d).length, solved: all.filter((p) => p.difficulty === d && p.solved).length });
  return {
    stats: { ...sheet.stats, solved: all.filter((p) => p.solved).length, easy: by('EASY'), medium: by('MEDIUM'), hard: by('HARD'), revision: all.filter((p) => p.revision).length },
    topics,
  };
}
