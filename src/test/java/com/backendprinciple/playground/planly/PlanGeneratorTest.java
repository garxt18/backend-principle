package com.backendprinciple.playground.planly;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.backendprinciple.playground.planly.PlanGenerator.Allocation;
import com.backendprinciple.playground.planly.PlanGenerator.TopicSlot;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class PlanGeneratorTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 28);
    private static final Set<DayOfWeek> MON_TO_SAT = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.SATURDAY);
    private static final Set<DayOfWeek> EVERY_DAY = EnumSet.allOf(DayOfWeek.class);

    @Test
    void fillsDaysInOrderAndSplitsBigTopics() {
        var s = PlanGenerator.generate(List.of(new TopicSlot(1, 60), new TopicSlot(2, 150), new TopicSlot(3, 60)), 120,
                EVERY_DAY, MONDAY);

        assertThat(s.allocations()).containsExactly(
                new Allocation(1, MONDAY, 0, 60),
                new Allocation(2, MONDAY, 1, 60),               // 1h left today ...
                new Allocation(2, MONDAY.plusDays(1), 0, 90),   // ... the rest tomorrow
                new Allocation(3, MONDAY.plusDays(1), 1, 30),
                new Allocation(3, MONDAY.plusDays(2), 0, 30));
        assertThat(s.endDate()).isEqualTo(MONDAY.plusDays(2));
    }

    @Test
    void skipsRestDays() {
        var s = PlanGenerator.generate(List.of(new TopicSlot(1, 7 * 60)), 60, MON_TO_SAT, MONDAY);
        assertThat(s.allocations()).extracting(Allocation::date).doesNotContain(MONDAY.plusDays(6)); // Sunday
        assertThat(s.endDate()).isEqualTo(MONDAY.plusDays(7)); // next Monday
    }

    @Test
    void neverStartsATopicWithOnlyASliverOfTheDayLeft() {
        var s = PlanGenerator.generate(List.of(new TopicSlot(1, 100), new TopicSlot(2, 60)), 120, EVERY_DAY, MONDAY);
        // 20 minutes left on Monday is not worth starting topic 2 - it goes to Tuesday.
        assertThat(s.allocations()).containsExactly(new Allocation(1, MONDAY, 0, 100), new Allocation(2, MONDAY.plusDays(1), 0, 60));
    }

    @Test
    void neverPlansMoreThanTheDailyBudget() {
        List<TopicSlot> topics = List.of(new TopicSlot(1, 13 * 60), new TopicSlot(2, 120), new TopicSlot(3, 21 * 60));
        var s = PlanGenerator.generate(topics, 150, MON_TO_SAT, MONDAY);
        Map<LocalDate, Integer> perDay = s.allocations().stream()
                .collect(Collectors.groupingBy(Allocation::date, Collectors.summingInt(Allocation::minutes)));
        assertThat(perDay.values()).allMatch(m -> m <= 150);
        assertThat(s.allocations().stream().mapToInt(Allocation::minutes).sum()).isEqualTo(36 * 60);
    }

    @Test
    void deadlineModeFindsTheDailyTimeThatFinishesOnTime() {
        // 42 hours of Java Basics in one week of Mon-Sat study.
        List<TopicSlot> topics = List.of(new TopicSlot(1, 20 * 60), new TopicSlot(2, 22 * 60));
        var s = PlanGenerator.fitDeadline(topics, MON_TO_SAT, MONDAY, MONDAY.plusDays(6));
        assertThat(s.minutesPerDay()).isEqualTo(7 * 60); // 42h / 6 days
        assertThat(s.endDate()).isBeforeOrEqualTo(MONDAY.plusDays(6));
    }

    @Test
    void countsStudyDaysAndFinishDates() {
        assertThat(PlanGenerator.countStudyDays(MONDAY, MONDAY.plusDays(13), MON_TO_SAT)).isEqualTo(12);
        assertThat(PlanGenerator.finishDate(MONDAY, 180, 60, MON_TO_SAT)).isEqualTo(MONDAY.plusDays(2));
    }

    @Test
    void emptyInputGivesEmptyPlanAndBadInputIsRejected() {
        assertThat(PlanGenerator.generate(List.of(), 60, EVERY_DAY, MONDAY).endDate()).isNull();
        assertThatThrownBy(() -> PlanGenerator.generate(List.of(new TopicSlot(1, 60)), 0, EVERY_DAY, MONDAY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PlanGenerator.generate(List.of(new TopicSlot(1, 60)), 60, Set.of(), MONDAY))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
