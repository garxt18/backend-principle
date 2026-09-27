package com.backendprinciple.playground.planly;

/** How a plan was sized: by the learner's daily time, or by the date they want to finish. */
public enum PaceMode {
    /** "I can study 2 hours a day" - the end date follows. */
    HOURS,
    /** "Finish Java Basics in 7 days" - the hours per day follow. */
    DEADLINE
}
