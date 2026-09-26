package com.backendprinciple.playground.progress;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** Pure function, no Spring, no database - trivial to unit test. */
public final class StreakCalculator {

    private StreakCalculator() {
    }

    /**
     * Consecutive days with at least one study session, ending today - or yesterday, so the streak
     * does not look broken in the morning before you have studied.
     */
    public static int currentStreak(Collection<LocalDate> studyDates, LocalDate today) {
        Set<LocalDate> days = new HashSet<>(studyDates);
        LocalDate cursor = days.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (days.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}
