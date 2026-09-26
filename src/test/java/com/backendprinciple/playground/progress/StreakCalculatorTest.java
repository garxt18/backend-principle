package com.backendprinciple.playground.progress;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class StreakCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 10);

    @Test
    void countsConsecutiveDaysEndingToday() {
        var days = List.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(2), TODAY.minusDays(4));
        assertThat(StreakCalculator.currentStreak(days, TODAY)).isEqualTo(3);
    }

    @Test
    void streakSurvivesUntilYouStudyToday() {
        var days = List.of(TODAY.minusDays(1), TODAY.minusDays(2));
        assertThat(StreakCalculator.currentStreak(days, TODAY)).isEqualTo(2);
    }

    @Test
    void missingYesterdayBreaksTheStreak() {
        assertThat(StreakCalculator.currentStreak(List.of(TODAY.minusDays(2)), TODAY)).isZero();
        assertThat(StreakCalculator.currentStreak(List.of(), TODAY)).isZero();
    }
}
