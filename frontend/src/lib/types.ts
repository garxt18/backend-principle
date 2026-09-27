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
export type ResourceKind = 'PLAYLIST' | 'VIDEO' | 'CHANNEL' | 'COURSE' | 'DOCS';

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
  /** Playlist levels: the lecture of the playlist that teaches this topic. */
  lectureNumber: number | null;
  /** Direct link to that lecture when known; null = open the playlist. */
  videoUrl: string | null;
}

export interface Playlist {
  name: string;
  channel: string;
  url: string;
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
  /** Set for the Java and Spring Boot levels, which are taught lecture-by-lecture by one playlist. */
  playlist: Playlist | null;
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

export type PaceMode = 'HOURS' | 'DEADLINE';
export type Weekday = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export interface PlanItem {
  topicId: number;
  topicTitle: string;
  levelNumber: number;
  levelTitle: string;
  lectureNumber: number | null;
  plannedHours: number;
  plannedDate: string;
  done: boolean;
}

export interface PlanDay {
  date: string;
  studyDay: boolean;
  plannedHours: number;
  minutesLogged: number;
  items: PlanItem[];
}

export interface Week {
  week: number;
  startDate: string;
  endDate: string;
  plannedHours: number;
  minutesLogged: number;
  days: PlanDay[];
  items: PlanItem[];
}

export interface Plan {
  id: string;
  name: string | null;
  paceMode: PaceMode;
  startDate: string;
  endDate: string;
  targetEndDate: string | null;
  hoursPerDay: number;
  hoursPerWeek: number;
  studyDays: Weekday[];
  totalWeeks: number;
  currentWeek: number;
  percentDone: number;
  totalHours: number;
  doneHours: number;
  /** positive = ahead of plan, negative = behind */
  scheduleDeltaHours: number;
  topicsTotal: number;
  topicsDone: number;
  projectedEndDate: string | null;
  today: PlanDay;
  overdue: PlanItem[];
  weeks: Week[];
}

export interface PlanPreview {
  topics: number;
  totalHours: number;
  hoursPerDay: number;
  studyDays: number;
  startDate: string;
  endDate: string;
  weeks: number;
  warning: string | null;
}

export interface PlanRequest {
  name?: string;
  startDate: string;
  pace: PaceMode;
  hoursPerDay?: number;
  targetEndDate?: string;
  studyDays: Weekday[];
  levelNumbers: number[];
  topicIds?: number[];
  skipCompleted: boolean;
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

export interface MyLink {
  id: number;
  levelId: number;
  topicId: number | null;
  title: string;
  url: string;
  note: string | null;
  createdAt: string;
}

export interface ResourceChoice {
  levelId: number;
  resourceId: number | null;
  userResourceId: number | null;
  chosenAt: string;
}

export interface MyResources {
  choices: ResourceChoice[];
  links: MyLink[];
}

export type FileLayer = string;
export type Track = 'BACKEND' | 'FRONTEND';

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
  track: Track;
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
  track: Track;
  buildOrder: number;
  positionInTrack: number;
  filesInTrack: number;
  lineCount: number;
  linesCompleted: number;
  lines: string[];
  notes: Record<string, string>;
  outline: FileOutline;
  previousFileId: string | null;
  nextFileId: string | null;
}

export interface FileOutline {
  purpose: string;
  items: { line: number; kind: string; name: string; summary: string }[];
}

export interface Explanation {
  lineNumber: number;
  code: string;
  kind: string;
  /** What the line does. */
  summary: string;
  /** Why you type it here / what breaks without it. */
  why: string;
  context: string;
  notes: { term: string; text: string }[];
}

export interface SyncResult {
  filesMatched: number;
  filesAdvanced: number;
  linesBefore: number;
  linesAfter: number;
  files: { path: string; before: number; after: number; lineCount: number; mismatchLine: number | null; expected: string | null; found: string | null }[];
  unmatched: string[];
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
