// Types mirror the Spring Boot DTOs (records) one-to-one.

export type Role = 'USER' | 'ADMIN';
export type Language = 'HINDI' | 'ENGLISH' | 'BOTH';

export interface User {
  id: string;
  email: string;
  displayName: string;
  role: Role;
  preferredLanguage: Language;
  enabled: boolean;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export type ResourceLang = 'HI' | 'EN';
export type ResourceKind = 'PLAYLIST' | 'VIDEO' | 'CHANNEL' | 'COURSE' | 'DOCS' | 'SEARCH';

export interface Resource {
  id: number;
  levelId: number;
  title: string;
  url: string;
  channel: string | null;
  language: ResourceLang;
  kind: ResourceKind;
  primaryPick: boolean;
  note: string | null;
  orderIndex: number;
}

export interface Topic {
  id: number;
  slug: string;
  title: string;
  description: string | null;
  practice: string | null;
  estimatedHours: number;
  orderIndex: number;
}

export interface Level {
  id: number;
  slug: string;
  levelNumber: number;
  title: string;
  summary: string;
  whyItMatters: string | null;
  projectTitle: string | null;
  projectDescription: string | null;
  suggestedMonth: number | null;
  totalHours: number;
  topics: Topic[];
  resources: Resource[];
}

export type TopicStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'DONE';

export interface TopicProgress {
  topicId: number;
  status: TopicStatus;
  confidence: number | null;
  notes: string | null;
  startedAt: string | null;
  completedAt: string | null;
  updatedAt: string;
}

export interface LevelProgress {
  levelId: number;
  levelNumber: number;
  title: string;
  topicsDone: number;
  topicsTotal: number;
  hoursDone: number;
  hoursTotal: number;
  percent: number;
}

export interface ProgressSummary {
  percent: number;
  topicsDone: number;
  topicsTotal: number;
  hoursDone: number;
  hoursTotal: number;
  currentStreakDays: number;
  minutesLast7Days: number;
  levels: LevelProgress[];
  topics: TopicProgress[];
}

export interface PlanItem {
  topicId: number;
  topicTitle: string;
  levelNumber: number;
  levelTitle: string;
  plannedHours: number;
  done: boolean;
}

export interface Week {
  week: number;
  startDate: string;
  endDate: string;
  plannedHours: number;
  minutesLogged: number;
  dsaTarget: number;
  dsaSolved: number;
  items: PlanItem[];
}

export interface Plan {
  id: string;
  startDate: string;
  endDate: string;
  hoursPerWeek: number;
  dsaPerWeek: number;
  totalWeeks: number;
  currentWeek: number;
  percentDone: number;
  scheduleDeltaHours: number;
  weeks: Week[];
}

export interface StudySession {
  id: number;
  topicId: number | null;
  date: string;
  minutes: number;
  note: string | null;
}

export interface DayMinutes {
  date: string;
  minutes: number;
}

export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD';

export interface DsaResource {
  title: string;
  url: string;
  channel: string | null;
  language: ResourceLang;
}

export interface DsaProblem {
  id: number;
  slug: string;
  title: string;
  difficulty: Difficulty;
  url: string;
  solved: boolean;
  revision: boolean;
  notes: string | null;
  solvedAt: string | null;
}

export interface DsaTopic {
  id: number;
  slug: string;
  title: string;
  summary: string | null;
  resources: DsaResource[];
  total: number;
  solved: number;
  problems: DsaProblem[];
}

export interface Count {
  total: number;
  solved: number;
}

export interface DsaSheet {
  stats: {
    total: number;
    solved: number;
    easy: Count;
    medium: Count;
    hard: Count;
    revision: number;
    solvedLast7Days: number;
  };
  topics: DsaTopic[];
}

export type FileLayer = string;

export interface LabProject {
  id: string;
  name: string;
  description: string | null;
  template: boolean;
  fileCount: number;
  totalLines: number;
  createdAt: string;
}

export interface LabFileEntry {
  id: string;
  path: string;
  language: string;
  layer: FileLayer;
  layerLabel: string;
  buildOrder: number;
  lineCount: number;
  linesCompleted: number;
  completed: boolean;
}

export interface LabProjectDetail {
  project: LabProject;
  linesCompleted: number;
  percent: number;
  layers: { layer: FileLayer; label: string; why: string }[];
  files: LabFileEntry[];
}

export interface LabFile {
  id: string;
  projectId: string;
  path: string;
  language: string;
  layer: FileLayer;
  layerLabel: string;
  layerWhy: string;
  buildOrder: number;
  lineCount: number;
  linesCompleted: number;
  lines: string[];
  notes: Record<string, string>;
  previousFileId: string | null;
  nextFileId: string | null;
}

export interface Explanation {
  lineNumber: number;
  code: string;
  kind: string;
  summary: string;
  context: string;
  notes: { term: string; text: string }[];
}

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface AdminStats {
  users: number;
  labProjects: number;
  activeLearnersLast7Days: number;
}
