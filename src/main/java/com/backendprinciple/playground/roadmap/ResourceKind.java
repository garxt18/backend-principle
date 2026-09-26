package com.backendprinciple.playground.roadmap;

public enum ResourceKind {
    PLAYLIST,
    VIDEO,
    CHANNEL,
    COURSE,
    DOCS,
    /** A YouTube search link - used where no single video is clearly the best, and never goes stale. */
    SEARCH
}
