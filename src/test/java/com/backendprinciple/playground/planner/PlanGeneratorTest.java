package com.backendprinciple.playground.planner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.backendprinciple.playground.planner.PlanGenerator.Allocation;
import com.backendprinciple.playground.planner.PlanGenerator.TopicSlot;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class PlanGeneratorTest {

    @Test
    void fillsWeeksInOrderAndSplitsBigTopics() {
        var schedule = PlanGenerator.generate(List.of(
                new TopicSlot(1, 6), new TopicSlot(2, 8), new TopicSlot(3, 3)), 10);

        assertThat(schedule.totalWeeks()).isEqualTo(2);
        assertThat(schedule.allocations()).containsExactly(
                new Allocation(1, 1, 0, 6),
                new Allocation(2, 1, 1, 4),   // topic 2 does not fit: 4h this week ...
                new Allocation(2, 2, 0, 4),   // ... and the remaining 4h next week
                new Allocation(3, 2, 1, 3));
    }

    @Test
    void exactFitDoesNotCreateAnEmptyTrailingWeek() {
        var schedule = PlanGenerator.generate(List.of(new TopicSlot(1, 5), new TopicSlot(2, 5)), 5);
        assertThat(schedule.totalWeeks()).isEqualTo(2);
    }

    @Test
    void neverPlansMoreThanTheWeeklyBudget() {
        List<TopicSlot> topics = List.of(new TopicSlot(1, 13), new TopicSlot(2, 2), new TopicSlot(3, 21), new TopicSlot(4, 7));
        var schedule = PlanGenerator.generate(topics, 6);

        Map<Integer, Integer> hoursPerWeek = schedule.allocations().stream()
                .collect(Collectors.groupingBy(Allocation::week, Collectors.summingInt(Allocation::hours)));
        assertThat(hoursPerWeek.values()).allMatch(h -> h <= 6);
        assertThat(schedule.allocations().stream().mapToInt(Allocation::hours).sum()).isEqualTo(43);
        assertThat(schedule.totalWeeks()).isEqualTo(8); // ceil(43 / 6)
    }

    @Test
    void emptyInputGivesEmptyPlan() {
        assertThat(PlanGenerator.generate(List.of(), 10).totalWeeks()).isZero();
    }

    @Test
    void rejectsNonPositiveBudget() {
        assertThatThrownBy(() -> PlanGenerator.generate(List.of(new TopicSlot(1, 1)), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
